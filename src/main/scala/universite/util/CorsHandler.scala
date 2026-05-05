package universite.util

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}

/**
 * Permet au front HTML/JS servi en local de consommer l'API sur un autre port.
 */
trait CorsHandler {

  private val corsHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`(
      "Origin", "X-Requested-With", "Content-Type", "Accept", "Authorization"
    )
  )

  def corsHandler(r: Route): Route = addCorsHeaders { preflightRequestHandler ~ r }

  private def addCorsHeaders: Directive0 = respondWithHeaders(corsHeaders)

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK)
      .withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE, PATCH)))
  }
}