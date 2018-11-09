package fr.xebia.rgpd

import java.util.{Properties, UUID}

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import cats.effect.IO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import fr.xebia.rgpd.model.{AmountUpdated, CreateUser, DeleteUser, Event, UpdateAmount, UserCreated, UserDeleted}
import io.circe.syntax._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}


class ProducerActor(transactor: Transactor[IO]) extends Actor with ActorLogging {

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

      ProducerActor.create(id, name).transact(transactor).unsafeRunSync()

      val userCreated: Event = UserCreated(id, 0)
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
      val name = ProducerActor.get(id).transact(transactor).unsafeRunSync()
      ProducerActor.update(id, s"${name.head}XXXXXX").transact(transactor).unsafeRunSync()

      val userAnon: Event = model.UserDeleted(id)
      val mess = new ProducerRecord[String, String](topic, id.toString, userAnon.asJson.noSpaces)
      producer.send(mess).get()
      log.info(s"UserDeleted sent")
  }
}

object ProducerActor {
  def props(transactor: Transactor[IO]): Props = Props(new ProducerActor(transactor))

  def create(id: UUID, name: String) = {
    sql"""
            INSERT INTO users (id, name)
            VALUES ($id, $name)
        """.update.run
  }

  def update(id: UUID, anon: String) = {
    sql"""
          UPDATE users
          SET name = $anon
          WHERE id = $id
      """.update.run
  }

  def get(id: UUID): doobie.ConnectionIO[String] = {
    sql"SELECT name FROM users WHERE id = $id".query[String].unique
  }
}
