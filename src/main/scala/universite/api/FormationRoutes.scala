package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import universite.api.JsonFormats._
import universite.model.{Filiere, Matiere}
import universite.service.FormationService

import scala.util.{Failure, Success}

/**
 * Routes pour le Module 3 : gestion des formations (filieres, matieres, UE).
 */
class FormationRoutes(service: FormationService = new FormationService) {

  val routes: Route = concat(

    // ======================================================
    //  FILIERES (sous /api/filieres)
    // ======================================================
    pathPrefix("filieres") {
      concat(
        pathEndOrSingleSlash {
          get {
            service.listerFilieres() match {
              case Success(ls) => complete(ls)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          } ~
          post {
            entity(as[Filiere]) { f =>
              service.enregistrerFiliere(f) match {
                case Success(saved) => complete(StatusCodes.Created, saved)
                case Failure(ex)    => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
              }
            }
          }
        },

        path(Segment) { id =>
          get {
            service.trouverFiliere(id) match {
              case Success(Some(f)) => complete(f)
              case Success(None)    => complete(StatusCodes.NotFound, ErreurReponse(s"Filiere $id introuvable"))
              case Failure(ex)      => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          } ~
          put {
            entity(as[Filiere]) { f =>
              service.enregistrerFiliere(f.copy(idFiliere = id)) match {
                case Success(s)  => complete(s)
                case Failure(ex) => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
              }
            }
          } ~
          delete {
            service.supprimerFiliere(id) match {
              case Success(0) => complete(StatusCodes.NotFound, ErreurReponse(s"Filiere $id introuvable"))
              case Success(_) => complete(SuccesReponse(s"Filiere $id supprimee"))
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        }
      )
    },

    // ======================================================
    //  MATIERES (sous /api/matieres)
    // ======================================================
    pathPrefix("matieres") {
      concat(
        pathEndOrSingleSlash {
          get {
            service.listerMatieres() match {
              case Success(ls) => complete(ls)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          } ~
          post {
            entity(as[Matiere]) { m =>
              service.enregistrerMatiere(m) match {
                case Success(saved) => complete(StatusCodes.Created, saved)
                case Failure(ex)    => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
              }
            }
          }
        },

        // GET /api/matieres/par-ue
        path("par-ue") {
          get {
            service.matieresParUe() match {
              case Success(map) => complete(map.toJson)
              case Failure(ex)  => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        },

        // GET /api/matieres/stats
        path("stats") {
          get {
            val resultat = for {
              total      <- service.listerMatieres().map(_.size)
              parUe      <- service.matieresParUe().map(_.view.mapValues(_.size).toMap)
              volume     <- service.volumeHoraireParUe()
              coef       <- service.coefficientParUe()
              ues        <- service.uesUniques()
            } yield JsObject(
              "total"               -> JsNumber(total),
              "ues"                 -> ues.toList.toJson,
              "matieresParUe"       -> parUe.toJson,
              "volumeHoraireParUe"  -> volume.toJson,
              "coefficientParUe"    -> coef.toJson
            )
            resultat match {
              case Success(j)  => complete(j)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        },

        path(Segment) { id =>
          get {
            service.trouverMatiere(id) match {
              case Success(Some(m)) => complete(m)
              case Success(None)    => complete(StatusCodes.NotFound, ErreurReponse(s"Matiere $id introuvable"))
              case Failure(ex)      => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          } ~
          put {
            entity(as[Matiere]) { m =>
              service.enregistrerMatiere(m.copy(idMatiere = id)) match {
                case Success(s)  => complete(s)
                case Failure(ex) => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
              }
            }
          } ~
          delete {
            service.supprimerMatiere(id) match {
              case Success(0) => complete(StatusCodes.NotFound, ErreurReponse(s"Matiere $id introuvable"))
              case Success(_) => complete(SuccesReponse(s"Matiere $id supprimee"))
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        }
      )
    }

  )
}
