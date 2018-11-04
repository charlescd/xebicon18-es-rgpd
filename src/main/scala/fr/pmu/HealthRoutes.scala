package fr.pmu

import java.lang.management.ManagementFactory

import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class HealthRoutes(implicit ec: ExecutionContext, log: LoggingAdapter) {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

  val routes: Route =
    path("health") {
      get {
        log.debug("/health executed")
        complete(Status(Duration(ManagementFactory.getRuntimeMXBean.getUptime, MILLISECONDS).toString()))
      }
    }
}
