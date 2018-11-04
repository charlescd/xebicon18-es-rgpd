package fr.pmu

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
//import akka.http.scaladsl.model.HttpMethods._
//import akka.http.scaladsl.model.headers._
//import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
//import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import cats.effect.IO
import doobie._
import doobie.implicits._
import io.circe.generic.auto._
import io.circe.parser._
import akka.pattern.{ask, pipe}
import scala.concurrent.duration._
//import cats._
//import cats.effect._
//import cats.implicits._

object System {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executor = system.dispatcher
  implicit val log = Logging(system, "rgpd")
  implicit val cs = IO.contextShift(executor)
  implicit val timeout = Timeout(5.seconds)
}

object Main extends App {

  import System._

  val hikariTransactor = Transactor.fromDriverManager[IO](
    driver = s"org.postgresql.Driver",
    url = s"jdbc:postgresql://localhost:5432/rgpd",
    user = "rgpd",
    pass = "rgpd"
  )

  val usersTable =
    sql"""
          CREATE TABLE IF NOT EXISTS users (
            id UUID PRIMARY KEY,
            name VARCHAR NOT NULL,
            amount BIGINT NOT NULL
          )
      """.update.run

  val res = usersTable.transact(hikariTransactor).unsafeRunSync()
  log.info(s"Tables created")

  val producerAndState = system.actorOf(ProducerAndState.props(log), "acc")
  new EventConsumer(producerAndState).run()

  val healthRoutes = new HealthRoutes().routes
  val appRoutes = new AppRoutes(producerAndState, hikariTransactor).routes
  val routes = healthRoutes ~ appRoutes

  Http().bindAndHandle(routes, "0.0.0.0", 9000)
}
