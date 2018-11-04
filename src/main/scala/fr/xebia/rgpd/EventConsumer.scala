package fr.xebia.rgpd

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import fr.xebia.rgpd.model.Event
import io.circe.parser.parse
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.Future

class EventConsumer(stateActor: ActorRef)(implicit system: ActorSystem, m: Materializer, timeout: Timeout) {

  val config = system.settings.config.getConfig("akka.kafka.consumer")
  val consumerSettings =
    ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9092")
      .withGroupId("rgpd")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")

  val consumer = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("rgpd"))
    .map { mess =>
      for {
        json <- parse(mess.value())
        event <- json.as[Event]
      } yield stateActor ! event

    }

  def run(): Future[Done] = consumer.runWith(Sink.ignore)
}
