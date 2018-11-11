package fr.xebia.rgpd

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import fr.xebia.rgpd.model.{CreateUser, DeleteUser, GetUser, UpdateAmount, User}
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext

class AppRoutes(producerActor: ActorRef, stateActor: ActorRef)(implicit ec: ExecutionContext, timeout: Timeout) {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

  val routes: Route =
    path("create") {
      post {
        entity(as[CreateUser]) { newUser =>
          onSuccess((producerActor ? newUser).mapTo[UUID]) { id =>
            complete(id)
          }
        }
      }
    } ~ path("get" / JavaUUID) { id =>
      get {
        onSuccess((stateActor ? GetUser(id)).mapTo[Option[User]]) { user =>
          user.map(complete(_)).getOrElse(complete(404, s"$id not found"))
        }
      }
    } ~ path("update") {
      post {
        entity(as[UpdateAmount]) { updateAmount =>
          producerActor ! updateAmount
          complete("OK")
        }
      }
    } ~ path("delete" / JavaUUID) { id =>
      delete {
        producerActor ! DeleteUser(id)
        complete("OK")
      }
    }
}