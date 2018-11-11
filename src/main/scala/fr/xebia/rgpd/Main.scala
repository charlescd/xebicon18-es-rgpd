package fr.xebia.rgpd

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{DELETE, GET, OPTIONS, POST, PUT}
import akka.http.scaladsl.model.headers.{`Access-Control-Allow-Credentials`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cats.effect.IO
import doobie._
import doobie.implicits._

import scala.concurrent.duration._

object System {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executor = system.dispatcher
  implicit val log = Logging(system, "rgpd")
  implicit val cs = IO.contextShift(executor)
  implicit val timeout = Timeout(5.seconds)
}

object Main extends App {

  private val preflightRequestHandler = options {
    complete(HttpResponse(StatusCodes.OK).withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)))
  }

  private def cors(route: Route): Route = {
    respondWithHeaders(
      `Access-Control-Allow-Origin`.`*`,
      `Access-Control-Allow-Credentials`(true),
      `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With")
    ) {
      preflightRequestHandler ~ route
    }
  }

  import System._

  val postgresServer = system.settings.config.getString("postgres.server")
  val hikariTransactor = Transactor.fromDriverManager[IO](
    driver = s"org.postgresql.Driver",
    url = s"jdbc:postgresql://$postgresServer/rgpd",
    user = "rgpd",
    pass = "rgpd"
  )

  val keysTable =
    sql"""
          CREATE TABLE IF NOT EXISTS keys (
            id UUID PRIMARY KEY,
            key VARCHAR NOT NULL
          )
      """.update.run
  val res = keysTable.transact(hikariTransactor).unsafeRunSync()
  log.info(s"Tables created")

  val producerActor = system.actorOf(ProducerActor.props(hikariTransactor), "producer-actor")
  val stateActor = system.actorOf(StateActor.props(hikariTransactor), "state-actor")

  new EventConsumer(stateActor).run()

  val appRoutes = new AppRoutes(producerActor, stateActor).routes
  val routes = cors {
    appRoutes
  }

  Http().bindAndHandle(routes, "0.0.0.0", 9000)
}