package fr.pmu

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import io.circe.generic.auto._
import io.circe.parser.parse
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.Future

class EventConsumer(acc: ActorRef)(implicit system: ActorSystem, m: Materializer, timeout: Timeout, log: LoggingAdapter) {

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
      log.info(s"Read from kafka : ${mess.value()}")
      val a = parse(mess.value())

      a.flatMap(_.as[UserCreated].map(acc ! _))
        .getOrElse(a.flatMap(_.as[AmountUpdated].map(acc ! _)).right.get)

    }

  def run(): Future[Done] = consumer.runWith(Sink.foreach(o => log.info(s"$o")))
}
