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
      sender() ! state.get(id.toString)

    case UserCreated(id, _) =>
      val maybeName = StateActor.get(id).transact(transactor).attempt.unsafeRunSync()
      val newState = maybeName match {
        case Right(name) => state + (id.toString -> User(id, name, 0))
        case _ => state
      }
      log.info(s"UserCreated")
      context become active(newState)

    case AmountUpdated(id, amount) =>
      val user = state(id.toString)
      val newState = state + (id.toString -> user.copy(amount = user.amount + amount))
      log.info(s"AmountUpdated")
      context become active(newState)

    case UserDeleted(id) =>
      val user = state(id.toString)
      val maybeName = StateActor.get(id).transact(transactor).attempt.unsafeRunSync()
      val newState = maybeName match {
        case Right(name) => state + (id.toString -> User(id, name, user.amount))
        case _ => state
      }
      log.info(s"UserDeleted: ${id.toString}")
      context become active(newState)
  }

  override def receive: Receive = active(Map.empty)

}

object StateActor {
  def props(transactor: Transactor[IO]): Props = Props(new StateActor(transactor))

  def get(id: UUID): doobie.ConnectionIO[String] = {
    sql"SELECT name FROM users WHERE id = $id".query[String].unique
  }
}