package fr.xebia.rgpd

import java.util.UUID

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import cats.effect.IO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import fr.xebia.rgpd.model.{AmountUpdated, GetUser, User, UserCreated, UserDeleted}

class StateActor(transactor: Transactor[IO]) extends Actor with ActorLogging {

  override val supervisorStrategy = OneForOneStrategy() {
    case e =>
      log.error(s"Error : $e")
      log.info("Skipping")
      Resume
  }

  def active(state: Map[String, User]): Receive = {
    case GetUser(id) =>
      val maybeKey = StateActor.getKey(id).transact(transactor).attempt.unsafeRunSync()
      val user = state.get(id.toString).map { encryptedUser =>
        maybeKey match {
          case Right(key) => encryptedUser.copy(name = Encryption.decrypt(key, encryptedUser.name))
          case _ => encryptedUser
        }
      }
      sender() ! user

    case UserCreated(id, name, _) =>
      val user = User(id, name, 0)
      val newState = state + (id.toString -> user)
      log.info(s"UserCreated: $user")
      context become active(newState)

    case AmountUpdated(id, amount) =>
      val user = state(id.toString)
      val newState = state + (id.toString -> user.copy(amount = user.amount + amount))
      log.info(s"AmountUpdated")
      context become active(newState)

    case UserDeleted(id) =>
      log.info(s"UserDeleted: ${id.toString}")
  }

  override def receive: Receive = active(Map.empty)

}

object StateActor {
  def props(transactor: Transactor[IO]): Props = Props(new StateActor(transactor))

  def getKey(id: UUID): doobie.ConnectionIO[String] = {
    sql"SELECT key FROM keys WHERE id = $id".query[String].unique
  }
}