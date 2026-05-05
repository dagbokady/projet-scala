package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import universite.api.JsonFormats._
import universite.model.SeanceCours
import universite.service.EmploiDuTempsService

import scala.util.{Failure, Success}

class SeanceRoutes(service: EmploiDuTempsService = new EmploiDuTempsService) {

  val routes: Route = pathPrefix("seances") {
    concat(
      pathEndOrSingleSlash {
        get {
          service.listerToutes() match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        } ~
        post {
          entity(as[SeanceCours]) { s =>
            service.creer(s) match {
              case Success(seance) => complete(StatusCodes.Created, seance)
              case Failure(ex)     =>
                complete(StatusCodes.Conflict, ErreurReponse("Conflit ou seance invalide", Some(ex.getMessage)))
            }
          }
        }
      },

      // GET /seances/classe?filiere=...&niveau=...
      path("classe") {
        get {
          parameters("filiere", "niveau") { (f, n) =>
            service.emploiDeClasse(f, n) match {
              case Success(ls) => complete(ls)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      // GET /seances/enseignant/{id}
      path("enseignant" / Segment) { idEns =>
        get {
          service.emploiEnseignant(idEns) match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /seances/salle/{id}
      path("salle" / Segment) { idSalle =>
        get {
          service.emploiSalle(idSalle) match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /seances/conflits
      path("conflits") {
        get {
          service.detecterTousConflits() match {
            case Success(ls) =>
              val arr = JsArray(ls.map { case (a, b, raison) =>
                JsObject(
                  "seanceA" -> a.toJson,
                  "seanceB" -> b.toJson,
                  "raison"  -> JsString(raison)
                )
              }.toVector)
              complete(arr)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // POST /seances/verifier
      path("verifier") {
        post {
          entity(as[SeanceCours]) { s =>
            service.detecterConflitsAvec(s) match {
              case Success(Nil) =>
                complete(JsObject("conflit" -> JsBoolean(false), "message" -> JsString("Aucun conflit")))
              case Success(ls)  =>
                val arr = JsArray(ls.map { case (autre, raison) =>
                  JsObject("seance" -> autre.toJson, "raison" -> JsString(raison))
                }.toVector)
                complete(JsObject("conflit" -> JsBoolean(true), "conflits" -> arr))
              case Failure(ex)  => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      // PUT /seances/{id}
      path(Segment) { id =>
        put {
          entity(as[SeanceCours]) { s =>
            service.modifier(s.copy(idSeance = id)) match {
              case Success(seance) => complete(seance)
              case Failure(ex)     => complete(StatusCodes.Conflict, ErreurReponse(ex.getMessage))
            }
          }
        } ~
        delete {
          service.supprimer(id) match {
            case Success(n) if n > 0 => complete(SuccesReponse(s"Seance $id supprimee"))
            case Success(_)          => complete(StatusCodes.NotFound, ErreurReponse(s"Seance $id introuvable"))
            case Failure(ex)         => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      }
    )
  }
}
