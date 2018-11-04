package fr.pmu

import java.util.UUID

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.util.Timeout
//import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{handleExceptions, _}
import akka.http.scaladsl.server.Route
import cats.effect.IO
import doobie.util.transactor.Transactor
import io.circe.generic.auto._
import akka.pattern.{ask, pipe}
//import doobie._
//import doobie.implicits._
//import cats._
//import cats.effect._
//import cats.implicits._

import scala.concurrent.ExecutionContext

class AppRoutes(producerAndState: ActorRef, transactor: Transactor[IO])(implicit ec: ExecutionContext, timeout: Timeout, log: LoggingAdapter) {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._


  val routes: Route =
    path("create") {
      post {
        entity(as[NewUser]) { newUser =>
          onSuccess((producerAndState ? newUser).mapTo[String]) { id =>
            complete(id)
          }
        }
      }
    } ~ path("get" / JavaUUID) { id =>
      get {
        onSuccess((producerAndState ? id.toString).mapTo[User]) { user =>
          complete(user)
        }
      }
    } ~ path("amount") {
      post {
        entity(as[Transaction]) { transaction =>
          producerAndState ! transaction
          complete("OK")
        }
      }
    }
}

object AppRoutes {
  //  def createUser(name: String) = {
  //    val id = UUID.randomUUID().toString
  //
  //    sql"""
  //          INSERT INTO users (id, username, password, mail, last_name, first_name, skills, twitter, github, google, facebook, role, account_type, timestamp)
  //          VALUES ($id, ${user.username}, $password, ${user.mail}, ${user.lastName}, ${user.firstName}, ${user.skills}, ${user.twitter}, ${user.github}, ${user.google}, ${user.facebook}, $role, $accountType, $timestamp)
  //      """.update
  //  }
  //
  //  def update(id: UUID, amount: Int) =
  //    sql"""
  //         UPDATE users
  //         SET amount = $amount
  //         WHERE id = $id
  //       """.update.run
  //
  //  val get = sql"select 42".query[Int].unique
}