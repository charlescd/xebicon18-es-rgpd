package fr.xebia.rgpd

import java.util.{Properties, UUID}

import akka.actor.{Actor, ActorLogging, Props}
import fr.xebia.rgpd.model.{AmountUpdated, CreateUser, DeleteUser, Event, UpdateAmount, UserCreated, UserDeleted}
import io.circe.syntax._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

class ProducerActor() extends Actor with ActorLogging {

  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  val producer = new KafkaProducer[String, String](props)

  override def receive: Receive = {
    case CreateUser(name) =>
      val id = UUID.randomUUID().toString
      val userCreated: Event = UserCreated(id, name, 0)
      val mess = new ProducerRecord[String, String]("rgpd", id, userCreated.asJson.noSpaces)
      producer.send(mess).get()
      log.info(s"CreateUser sent")
      sender() ! id

    case UpdateAmount(id, amount) =>
      val amountUpdated: Event = AmountUpdated(id.toString, amount)
      val mess = new ProducerRecord[String, String]("rgpd", id.toString, amountUpdated.asJson.noSpaces)
      producer.send(mess).get()
      log.info(s"UpdateAmount sent")

    case DeleteUser(id) =>
      val userDelete: Event = UserDeleted(id.toString)
      val mess = new ProducerRecord[String, String]("rgpd", id.toString, userDelete.asJson.noSpaces)
      producer.send(mess).get()
      log.info(s"UserDeleted sent")
  }
}

object ProducerActor {
  def props: Props = Props(new ProducerActor())
}
