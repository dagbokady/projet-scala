package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import universite.api.JsonFormats._
import universite.model.{DecisionAcademique, Note}
import universite.service.NoteService

import scala.util.{Failure, Success}

class NoteRoutes(service: NoteService = new NoteService) {

  val routes: Route = pathPrefix("notes") {
    concat(
      // GET /notes  -> liste toutes les notes
      pathEndOrSingleSlash {
        get {
          service.listerToutes() match {
            case Success(notes) => complete(notes)
            case Failure(ex)    => complete(StatusCodes.InternalServerError, ErreurReponse("Echec lecture", Some(ex.getMessage)))
          }
        } ~
        // POST /notes -> creer/mettre a jour une note
        post {
          entity(as[Note]) { note =>
            service.saisir(note) match {
              case Success(n)  => complete(StatusCodes.Created, n)
              case Failure(ex) => complete(StatusCodes.BadRequest, ErreurReponse("Saisie refusee", Some(ex.getMessage)))
            }
          }
        }
      },

      // GET /notes/etudiant/{matricule}
      path("etudiant" / Segment) { matricule =>
        get {
          service.listerParEtudiant(matricule) match {
            case Success(ns) => complete(ns)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /notes/matiere/{idMatiere}
      path("matiere" / Segment) { idMat =>
        get {
          service.listerParMatiere(idMat) match {
            case Success(ns) => complete(ns)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /notes/moyenne/{matricule} -> moyenne ponderee
      path("moyenne" / Segment) { matricule =>
        get {
          service.moyenneGenerale(matricule) match {
            case Success(Some(m)) =>
              val dec = DecisionAcademique.fromMoyenne(m)
              complete(JsObject(
                "matricule" -> JsString(matricule),
                "moyenne"   -> JsNumber(BigDecimal(m).setScale(2, BigDecimal.RoundingMode.HALF_UP)),
                "decision"  -> JsString(DecisionAcademique.toString(dec))
              ))
            case Success(None) =>
              complete(StatusCodes.NotFound, ErreurReponse(s"Aucune note trouvee pour $matricule"))
            case Failure(ex) =>
              complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /notes/releve/{matricule}
      path("releve" / Segment) { matricule =>
        get {
          service.releveDeNotes(matricule) match {
            case Success(rel) =>
              val arr = JsArray(rel.map { case (n, _) => n.toJson }.toVector)
              service.moyenneGenerale(matricule) match {
                case Success(opt) =>
                  complete(JsObject(
                    "matricule" -> JsString(matricule),
                    "notes"     -> arr,
                    "moyenne"   -> opt.map(m => JsNumber(BigDecimal(m).setScale(2, BigDecimal.RoundingMode.HALF_UP))).getOrElse(JsNull),
                    "decision"  -> opt.map(m => JsString(DecisionAcademique.toString(DecisionAcademique.fromMoyenne(m)))).getOrElse(JsNull)
                  ))
                case Failure(_) => complete(JsObject("matricule" -> JsString(matricule), "notes" -> arr))
              }
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /notes/classement
      path("classement") {
        get {
          service.classement() match {
            case Success(cl) =>
              val arr = JsArray(cl.zipWithIndex.map { case ((mat, m), i) =>
                JsObject(
                  "rang"      -> JsNumber(i + 1),
                  "matricule" -> JsString(mat),
                  "moyenne"   -> JsNumber(BigDecimal(m).setScale(2, BigDecimal.RoundingMode.HALF_UP)),
                  "decision"  -> JsString(DecisionAcademique.toString(DecisionAcademique.fromMoyenne(m)))
                )
              }.toVector)
              complete(arr)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /notes/echec
      path("echec") {
        get {
          service.etudiantsEnEchec() match {
            case Success(ls) =>
              val arr = JsArray(ls.map { case (m, moy, d) =>
                JsObject(
                  "matricule" -> JsString(m),
                  "moyenne"   -> JsNumber(BigDecimal(moy).setScale(2, BigDecimal.RoundingMode.HALF_UP)),
                  "decision"  -> JsString(DecisionAcademique.toString(d))
                )
              }.toVector)
              complete(arr)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /notes/incompletes  GET /notes/invalides
      path("incompletes") {
        get {
          service.notesIncompletes() match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },
      path("invalides") {
        get {
          service.notesInvalides() match {
            case Success(ls) => complete(ls)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // GET /notes/stats/parmatiere -> moyenne par matiere
      path("stats" / "parmatiere") {
        get {
          service.moyennesParMatiere() match {
            case Success(map) =>
              val arr = JsArray(map.toList.sortBy(-_._2).map { case (m, mo) =>
                JsObject(
                  "matiere" -> JsString(m),
                  "moyenne" -> JsNumber(BigDecimal(mo).setScale(2, BigDecimal.RoundingMode.HALF_UP))
                )
              }.toVector)
              complete(arr)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      // DELETE /notes/{id}
      path(Segment) { id =>
        delete {
          service.supprimer(id) match {
            case Success(n) if n > 0 => complete(SuccesReponse(s"Note $id supprimee"))
            case Success(_)          => complete(StatusCodes.NotFound, ErreurReponse(s"Note $id introuvable"))
            case Failure(ex)         => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      }
    )
  }
}
