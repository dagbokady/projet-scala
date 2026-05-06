package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import universite.api.JsonFormats._
import universite.model.Enseignant
import universite.service.EnseignantService

import scala.util.{Failure, Success}

/**
 * Routes pour le Module 2 : gestion des enseignants.
 */
class EnseignantRoutes(service: EnseignantService = new EnseignantService) {

  val routes: Route = pathPrefix("enseignants") {
    concat(

      pathEndOrSingleSlash {
        get {
          parameters("departement".?) {
            case Some(d) => service.filtrerParDepartement(d) match {
              case Success(ls) => complete(ls)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
            case None => service.listerTous() match {
              case Success(ls) => complete(ls)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        } ~
        post {
          entity(as[Enseignant]) { e =>
            service.enregistrer(e) match {
              case Success(saved) => complete(StatusCodes.Created, saved)
              case Failure(ex)    => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      // GET /api/enseignants/stats
      path("stats") {
        get {
          val resultat = for {
            tous   <- service.listerTous()
            parDep <- service.comptageParDepartement()
            top    <- service.topVolumeHoraire(5)
          } yield JsObject(
            "total"           -> JsNumber(tous.size),
            "parDepartement"  -> parDep.toJson,
            "topVolume"       -> top.map { case (id, h) =>
              JsObject("idEnseignant" -> JsString(id), "heures" -> JsNumber(h))
            }.toJson
          )
          resultat match {
            case Success(j)  => complete(j)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /api/enseignants/{id}/cours
      path(Segment / "cours") { id =>
        get {
          service.coursAssures(id) match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /api/enseignants/{id}
      path(Segment) { id =>
        get {
          service.trouverParId(id) match {
            case Success(Some(e)) => complete(e)
            case Success(None)    => complete(StatusCodes.NotFound, ErreurReponse(s"Enseignant $id introuvable"))
            case Failure(ex)      => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        } ~
        put {
          entity(as[Enseignant]) { e =>
            service.enregistrer(e.copy(idEnseignant = id)) match {
              case Success(saved) => complete(saved)
              case Failure(ex)    => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
            }
          }
        } ~
        delete {
          service.supprimer(id) match {
            case Success(0) => complete(StatusCodes.NotFound, ErreurReponse(s"Enseignant $id introuvable"))
            case Success(_) => complete(SuccesReponse(s"Enseignant $id supprime"))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      }

    )
  }
}
