package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import universite.api.JsonFormats._
import universite.service.BigDataService

import scala.util.{Failure, Success}

/**
 * Routes pour le Module 10 : Big Data.
 *
 * Sous /api/bigdata.
 */
class BigDataRoutes(service: BigDataService = new BigDataService) {

  val routes: Route = pathPrefix("bigdata") {
    concat(

      // POST /api/bigdata/export : declenche tous les exports
      path("export") {
        post {
          service.exporterTout() match {
            case Success(r) => complete(JsObject(
              "rapportAcademique"     -> JsString(r.rapportAcademique),
              "performances"          -> JsString(r.performances),
              "indicateursFinanciers" -> JsString(r.indicateursFinanciers),
              "message"               -> JsString("Export termine. Fichiers generes dans ./output/")
            ))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("manquantes") {
        get {
          service.detecterValeursManquantes() match {
            case Success(m) =>
              complete(JsObject(m.map { case (k, v) => k -> JsNumber(v) }))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("promotion") {
        get {
          service.performanceParPromotion() match {
            case Success(m) =>
              val obj = JsObject(m.map { case (k, v) =>
                k -> JsNumber(BigDecimal(v).setScale(2, BigDecimal.RoundingMode.HALF_UP))
              })
              complete(obj)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("absences-mois") {
        get {
          service.tendanceAbsencesParMois() match {
            case Success(m) =>
              complete(JsObject(m.map { case (k, v) => k -> JsNumber(v) }))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("paiements-mois") {
        get {
          service.tendancePaiementsParMois() match {
            case Success(m) =>
              complete(JsObject(m.map { case (k, v) =>
                k -> JsNumber(BigDecimal(v).setScale(2, BigDecimal.RoundingMode.HALF_UP))
              }))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      }

    )
  }
}