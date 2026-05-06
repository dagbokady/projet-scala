package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import universite.api.JsonFormats._
import universite.model.Paiement
import universite.service.PaiementService

import scala.util.{Failure, Success}

/**
 * Routes pour le Module 8 : gestion des paiements.
 */
class PaiementRoutes(service: PaiementService = new PaiementService) {

  val routes: Route = pathPrefix("paiements") {
    concat(

      pathEndOrSingleSlash {
        get {
          parameters("matricule".?) {
            case Some(m) => service.listerParEtudiant(m) match {
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
          entity(as[Paiement]) { p =>
            service.enregistrer(p) match {
              case Success(saved) => complete(StatusCodes.Created, saved)
              case Failure(ex)    => complete(StatusCodes.BadRequest, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      // GET /api/paiements/dette
      path("dette") {
        get {
          service.etudiantsEnDette() match {
            case Success(ls) =>
              complete(ls.map { case (m, d) =>
                JsObject("matricule" -> JsString(m), "dette" -> JsNumber(d))
              }.toJson)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /api/paiements/synthese
      path("synthese") {
        get {
          val res = for {
            attendu  <- service.totalAttendu()
            encaisse <- service.totalEncaisse()
            taux     <- service.tauxRecouvrement()
            parFil   <- service.syntheseParFiliere()
            mode     <- service.repartitionParMode()
          } yield JsObject(
            "totalAttendu"     -> JsNumber(attendu),
            "totalEncaisse"    -> JsNumber(encaisse),
            "totalRestant"     -> JsNumber(attendu - encaisse),
            "tauxRecouvrement" -> JsNumber(BigDecimal(taux).setScale(4, BigDecimal.RoundingMode.HALF_UP)),
            "parFiliere"       -> parFil.map { case (f, (du, paye, t)) =>
              f -> JsObject(
                "attendu"  -> JsNumber(du),
                "encaisse" -> JsNumber(paye),
                "restant"  -> JsNumber(du - paye),
                "taux"     -> JsNumber(BigDecimal(t).setScale(4, BigDecimal.RoundingMode.HALF_UP))
              )
            }.toMap.toJson,
            "parMode"          -> mode.toJson
          )
          res match {
            case Success(j)  => complete(j)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /api/paiements/etudiant/{matricule}/reste
      path("etudiant" / Segment / "reste") { matricule =>
        get {
          val res = for {
            paye  <- service.totalPayeEtudiant(matricule)
            reste <- service.resteEtudiant(matricule)
          } yield JsObject(
            "matricule"   -> JsString(matricule),
            "totalPaye"   -> JsNumber(paye),
            "resteAPayer" -> JsNumber(reste)
          )
          res match {
            case Success(j)  => complete(j)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path(Segment) { id =>
        get {
          service.trouverParId(id) match {
            case Success(Some(p)) => complete(p)
            case Success(None)    => complete(StatusCodes.NotFound, ErreurReponse(s"Paiement $id introuvable"))
            case Failure(ex)      => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        } ~
        delete {
          service.supprimer(id) match {
            case Success(0) => complete(StatusCodes.NotFound, ErreurReponse(s"Paiement $id introuvable"))
            case Success(_) => complete(SuccesReponse(s"Paiement $id supprime"))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      }

    )
  }
}
