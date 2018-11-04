package fr.pmu

import java.util.{Properties, UUID}

import akka.actor.{Actor, Props}
import akka.event.LoggingAdapter
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import io.circe.syntax._
import io.circe.generic.auto._

class ProducerAndState(log: LoggingAdapter) extends Actor {

  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  val producer = new KafkaProducer[String, String](props)

  def active(state: Map[String, User]): Receive = {
    case user: NewUser =>
      val id = UUID.randomUUID().toString
      val userCreated = UserCreated("user-created", id, user.name, 0)
      val mess = new ProducerRecord[String, String]("rgpd", userCreated.id, userCreated.asJson.noSpaces)
      producer.send(mess).get()
      log.info(s"NewUser sent")
      sender() ! id

    case transaction: Transaction =>
      val amountUpdated = AmountUpdated("amount-updated", transaction.id, transaction.amount)
      val mess = new ProducerRecord[String, String]("rgpd", amountUpdated.id, amountUpdated.asJson.noSpaces)
      producer.send(mess).get()
      log.info(s"Transaction sent")

    case UserCreated(_, id, name, _) =>
      val user = User(id, name, 0)
      val newState = state + (id -> user)
      log.info(s"UserCreated: $user")
      context become active(newState)

    case AmountUpdated(_, id, amount) =>
      val user = state(id)
      val newState = state + (id -> User(id, user.name, user.amount + amount))
      log.info(s"AmountUpdated")
      context become active(newState)

    case id: String =>
      sender() ! state(id)

    case a => println(s"FUCK $a")
  }

  override def receive: Receive = active(Map.empty)
}

object ProducerAndState {
  def props(log: LoggingAdapter): Props = Props(new ProducerAndState(log))
}
