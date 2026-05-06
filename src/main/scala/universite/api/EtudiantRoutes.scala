package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import universite.api.JsonFormats._
import universite.model.{Etudiant, StatutEtudiant}
import universite.service.EtudiantService

import scala.util.{Failure, Success}

/**
 * Routes pour le Module 1 : gestion des etudiants.
 */
class EtudiantRoutes(service: EtudiantService = new EtudiantService) {

  val routes: Route = pathPrefix("etudiants") {
    concat(

      // GET /api/etudiants
      pathEndOrSingleSlash {
        get {
          parameters("filiere".?, "niveau".?, "statut".?) { (fil, niv, st) =>
            val res = (fil, niv, st) match {
              case (Some(f), _, _) => service.filtrerParFiliere(f)
              case (_, Some(n), _) => service.filtrerParNiveau(n)
              case (_, _, Some(s)) => service.filtrerParStatut(s)
              case _               => service.listerTous()
            }
            res match {
              case Success(ls) => complete(ls)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        } ~
        post {
          entity(as[Etudiant]) { e =>
            service.enregistrer(e) match {
              case Success(saved) => complete(StatusCodes.Created, saved)
              case Failure(ex)    => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      // GET /api/etudiants/stats : indicateurs simples
      path("stats") {
        get {
          val resultat = for {
            total      <- service.listerTous().map(_.size)
            actifs     <- service.compterActifs()
            suspendus  <- service.compterSuspendus()
            diplomes   <- service.compterDiplomes()
            parFil     <- service.comptageParFiliere()
            parNiv     <- service.comptageParNiveau()
          } yield JsObject(
            "total"      -> JsNumber(total),
            "actifs"     -> JsNumber(actifs),
            "suspendus"  -> JsNumber(suspendus),
            "diplomes"   -> JsNumber(diplomes),
            "parFiliere" -> parFil.toJson,
            "parNiveau"  -> parNiv.toJson
          )
          resultat match {
            case Success(j)  => complete(j)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /api/etudiants/{matricule}
      path(Segment) { matricule =>
        get {
          service.trouverParMatricule(matricule) match {
            case Success(Some(e)) => complete(e)
            case Success(None)    => complete(StatusCodes.NotFound, ErreurReponse(s"Etudiant $matricule introuvable"))
            case Failure(ex)      => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        } ~
        put {
          entity(as[Etudiant]) { e =>
            service.enregistrer(e.copy(matricule = matricule)) match {
              case Success(saved) => complete(saved)
              case Failure(ex)    => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
            }
          }
        } ~
        delete {
          service.supprimer(matricule) match {
            case Success(0)  => complete(StatusCodes.NotFound, ErreurReponse(s"Etudiant $matricule introuvable"))
            case Success(_)  => complete(SuccesReponse(s"Etudiant $matricule supprime"))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // PATCH /api/etudiants/{matricule}/statut?valeur=Actif
      path(Segment / "statut") { matricule =>
        patch {
          parameters("valeur") { valeur =>
            service.changerStatut(matricule, StatutEtudiant.fromString(valeur)) match {
              case Success(e)  => complete(e)
              case Failure(ex) => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
            }
          }
        }
      }

    )
  }
}
