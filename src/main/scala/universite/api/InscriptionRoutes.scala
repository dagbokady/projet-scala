package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import universite.api.JsonFormats._
import universite.model.{Inscription, StatutInscription}
import universite.service.InscriptionService

import scala.util.{Failure, Success}

/**
 * Routes pour le Module 4 : gestion des inscriptions.
 */
class InscriptionRoutes(service: InscriptionService = new InscriptionService) {

  val routes: Route = pathPrefix("inscriptions") {
    concat(

      pathEndOrSingleSlash {
        get {
          parameters("statut".?, "matricule".?) { (st, mat) =>
            val res = (st, mat) match {
              case (_, Some(m))           => service.listerParEtudiant(m)
              case (Some("Validee"),  _)  => service.listerValidees()
              case (Some("En attente"), _) | (Some("EnAttente"), _) => service.listerEnAttente()
              case (Some("Annulee"),  _)  => service.listerAnnulees()
              case _                       => service.listerToutes()
            }
            res match {
              case Success(ls) => complete(ls)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        } ~
        post {
          entity(as[Inscription]) { i =>
            service.inscrire(i) match {
              case Success(saved) => complete(StatusCodes.Created, saved)
              case Failure(ex)    => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      path("stats") {
        get {
          val res = for {
            total <- service.listerToutes().map(_.size)
            stat  <- service.comptageParStatut()
            annee <- service.comptageParAnnee()
            fil   <- service.comptageParFiliere()
          } yield JsObject(
            "total"      -> JsNumber(total),
            "parStatut"  -> stat.toJson,
            "parAnnee"   -> annee.toJson,
            "parFiliere" -> fil.toJson
          )
          res match {
            case Success(j)  => complete(j)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path(Segment / "statut") { id =>
        patch {
          parameters("valeur") { v =>
            service.changerStatut(id, StatutInscription.fromString(v)) match {
              case Success(i)  => complete(i)
              case Failure(ex) => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      path(Segment) { id =>
        get {
          service.trouverParId(id) match {
            case Success(Some(i)) => complete(i)
            case Success(None)    => complete(StatusCodes.NotFound, ErreurReponse(s"Inscription $id introuvable"))
            case Failure(ex)      => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        } ~
        delete {
          service.supprimer(id) match {
            case Success(0) => complete(StatusCodes.NotFound, ErreurReponse(s"Inscription $id introuvable"))
            case Success(_) => complete(SuccesReponse(s"Inscription $id supprimee"))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      }

    )
  }
}
