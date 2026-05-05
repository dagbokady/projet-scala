package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import universite.api.JsonFormats._
import universite.model.Absence
import universite.service.AbsenceService

import scala.util.{Failure, Success}

class AbsenceRoutes(service: AbsenceService = new AbsenceService) {

  val routes: Route = pathPrefix("absences") {
    concat(
      // GET /absences | POST /absences
      pathEndOrSingleSlash {
        get {
          service.listerToutes() match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        } ~
        post {
          entity(as[Absence]) { abs =>
            service.enregistrer(abs) match {
              case Success(a) => complete(StatusCodes.Created, a)
              case Failure(ex) => complete(StatusCodes.BadRequest, ErreurReponse("Saisie refusee", Some(ex.getMessage)))
            }
          }
        }
      },

      // GET /absences/etudiant/{matricule}
      path("etudiant" / Segment) { matricule =>
        get {
          service.listerParEtudiant(matricule) match {
            case Success(ls) =>
              service.totalHeures(matricule) match {
                case Success(t) =>
                  complete(JsObject(
                    "matricule"      -> JsString(matricule),
                    "totalHeures"    -> JsNumber(t),
                    "alerte"         -> JsBoolean(t >= service.seuilAlerte),
                    "absences"       -> ls.toJson
                  ))
                case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
              }
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /absences/matiere/{idMatiere}
      path("matiere" / Segment) { idMat =>
        get {
          service.listerParMatiere(idMat) match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /absences/non-justifiees
      path("non-justifiees") {
        get {
          service.absencesNonJustifiees() match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /absences/alerte?seuil=10
      path("alerte") {
        get {
          parameter("seuil".as[Int].withDefault(service.seuilAlerte)) { seuil =>
            service.etudiantsEnAlerte(seuil) match {
              case Success(ls) =>
                val arr = JsArray(ls.map { case (m, t) =>
                  JsObject("matricule" -> JsString(m), "totalHeures" -> JsNumber(t))
                }.toVector)
                complete(arr)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      // GET /absences/stats/filiere
      path("stats" / "filiere") {
        get {
          service.tauxParFiliere() match {
            case Success(map) =>
              val arr = JsArray(map.toList.sortBy(-_._2).map { case (f, t) =>
                JsObject(
                  "filiere"             -> JsString(f),
                  "moyenneHeuresEtu"    -> JsNumber(BigDecimal(t).setScale(2, BigDecimal.RoundingMode.HALF_UP))
                )
              }.toVector)
              complete(arr)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /absences/stats/matiere
      path("stats" / "matiere") {
        get {
          service.rapportParMatiere() match {
            case Success(map) =>
              val arr = JsArray(map.toList.sortBy(-_._2).map { case (m, h) =>
                JsObject("matiere" -> JsString(m), "totalHeures" -> JsNumber(h))
              }.toVector)
              complete(arr)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // PATCH /absences/{id}/justifier
      path(Segment / "justifier") { id =>
        patch {
          parameter("justifiee".as[Boolean].withDefault(true)) { just =>
            service.justifier(id, just) match {
              case Success(n) if n > 0 => complete(SuccesReponse(s"Absence $id mise a jour (justifiee=$just)"))
              case Success(_)          => complete(StatusCodes.NotFound, ErreurReponse(s"Absence $id introuvable"))
              case Failure(ex)         => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      // DELETE /absences/{id}
      path(Segment) { id =>
        delete {
          service.supprimer(id) match {
            case Success(n) if n > 0 => complete(SuccesReponse(s"Absence $id supprimee"))
            case Success(_)          => complete(StatusCodes.NotFound, ErreurReponse(s"Absence $id introuvable"))
            case Failure(ex)         => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      }
    )
  }
}
