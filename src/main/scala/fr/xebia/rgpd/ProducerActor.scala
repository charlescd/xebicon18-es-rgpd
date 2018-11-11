package fr.xebia.rgpd

import java.util.{Properties, UUID}

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import fr.xebia.rgpd.model.{AmountUpdated, CreateUser, DeleteUser, Event, UpdateAmount, UserCreated, UserDeleted}
import io.circe.syntax._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

class ProducerActor() extends Actor with ActorLogging {

  override val supervisorStrategy = OneForOneStrategy() {
    case e =>
      log.error(s"Error : $e")
      log.info("Skipping")
      Resume
  }

  val topic = context.system.settings.config.getString("kafka.topic")
  val bootstrapServer = context.system.settings.config.getString("kafka.bootstrap-server")
  val props = new Properties()
  props.put("bootstrap.servers", bootstrapServer)
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  val producer = new KafkaProducer[String, String](props)

  override def receive: Receive = {
    case CreateUser(name) =>
      val id = UUID.randomUUID()
      val userCreated: Event = UserCreated(id, name, 0)
      val mess = new ProducerRecord[String, String](topic, id.toString, userCreated.asJson.noSpaces)
      producer.send(mess).get()
      log.info(s"UserCreated sent")
      sender() ! id

    case UpdateAmount(id, amount) =>
      val amountUpdated: Event = AmountUpdated(id, amount)
      val mess = new ProducerRecord[String, String](topic, id.toString, amountUpdated.asJson.noSpaces)
      producer.send(mess).get()
      log.info(s"AmountUpdated sent")

    case DeleteUser(id) =>
      val userDelete: Event = UserDeleted(id)
      val mess = new ProducerRecord[String, String](topic, id.toString, userDelete.asJson.noSpaces)
      producer.send(mess).get()
      log.info(s"UserDeleted sent")
  }
}

object ProducerActor {
  def props: Props = Props(new ProducerActor())
}
