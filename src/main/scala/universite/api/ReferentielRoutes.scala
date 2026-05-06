package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import universite.api.JsonFormats._
import universite.repository._

import scala.util.{Failure, Success}

/**
 * Routes pour les referentiels - necessaires au front pour peupler les
 * listes deroulantes (selects) lors de la saisie.
 */
class ReferentielRoutes(
  etudiantRepo:   EtudiantRepository   = new EtudiantRepository,
  matiereRepo:    MatiereRepository    = new MatiereRepository,
  salleRepo:      SalleRepository      = new SalleRepository,
  enseignantRepo: EnseignantRepository = new EnseignantRepository,
  filiereRepo:    FiliereRepository    = new FiliereRepository
) {

  val routes: Route = pathPrefix("ref") {
    concat(

      path("etudiants") {
        get {
          etudiantRepo.listerTous() match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("matieres") {
        get {
          matiereRepo.listerToutes() match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("salles") {
        get {
          salleRepo.listerToutes() match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("enseignants") {
        get {
          enseignantRepo.listerTous() match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("filieres") {
        get {
          filiereRepo.listerToutes() match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      }

    )
  }
}
