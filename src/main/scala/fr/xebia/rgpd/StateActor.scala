package fr.xebia.rgpd

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import fr.xebia.rgpd.model.{AmountUpdated, GetUser, User, UserCreated, UserDeleted}

class StateActor() extends Actor with ActorLogging {

  override val supervisorStrategy = OneForOneStrategy() {
    case e =>
      log.error(s"Error : $e")
      log.info("Skipping")
      Resume
  }

  def active(state: Map[String, User]): Receive = {
    case GetUser(id) =>
      sender() ! state.get(id.toString)

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
      context become active(state - id.toString)
  }

  override def receive: Receive = active(Map.empty)

}

object StateActor {
  def props: Props = Props(new StateActor())
}