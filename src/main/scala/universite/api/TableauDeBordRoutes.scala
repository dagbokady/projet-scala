package universite.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import universite.api.JsonFormats._
import universite.model.DecisionAcademique
import universite.service.TableauDeBordService

import scala.util.{Failure, Success}

/**
 * Routes pour le Module 9 : tableau de bord academique.
 *
 * Sous /api/dashboard.
 */
class TableauDeBordRoutes(service: TableauDeBordService = new TableauDeBordService) {

  val routes: Route = pathPrefix("dashboard") {
    concat(

      // GET /api/dashboard/synthese : KPIs globaux
      path("synthese") {
        get {
          service.syntheseGlobale() match {
            case Success(s) =>
              complete(JsObject(
                "nbEtudiants"        -> JsNumber(s.nbEtudiants),
                "nbEnseignants"      -> JsNumber(s.nbEnseignants),
                "nbFilieres"         -> JsNumber(s.nbFilieres),
                "nbMatieres"         -> JsNumber(s.nbMatieres),
                "moyenneGlobale"     -> s.moyenneGlobale.map(m =>
                  JsNumber(BigDecimal(m).setScale(2, BigDecimal.RoundingMode.HALF_UP))
                ).getOrElse(JsNull),
                "tauxAbsenteisme"    -> s.tauxAbsenteisme.toJson,
                "tauxRecouvrement"   -> JsNumber(BigDecimal(s.tauxRecouvrement).setScale(4, BigDecimal.RoundingMode.HALF_UP)),
                "tauxReussiteGlobal" -> JsNumber(BigDecimal(s.tauxReussiteGlobal).setScale(4, BigDecimal.RoundingMode.HALF_UP))
              ))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("etudiants") {
        get {
          service.indicateursEtudiants() match {
            case Success(i) =>
              complete(JsObject(
                "total"     -> JsNumber(i.total),
                "actifs"    -> JsNumber(i.actifs),
                "suspendus" -> JsNumber(i.suspendus),
                "diplomes"  -> JsNumber(i.diplomes),
                "parFiliere"-> i.parFiliere.toJson,
                "parNiveau" -> i.parNiveau.toJson
              ))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("reussite") {
        get {
          val res = for {
            r        <- service.indicateursReussite()
            tauxFil  <- service.tauxReussiteParFiliere()
            topFil   <- service.filiereTopReussite()
          } yield JsObject(
            "moyenneGlobale" -> r.moyenneGlobale.map(m =>
              JsNumber(BigDecimal(m).setScale(2, BigDecimal.RoundingMode.HALF_UP))
            ).getOrElse(JsNull),
            "top5"           -> r.top5.map { case (mat, m) =>
              JsObject(
                "matricule"        -> JsString(mat),
                "moyenneGenerale"  -> JsNumber(BigDecimal(m).setScale(2, BigDecimal.RoundingMode.HALF_UP))
              )
            }.toJson,
            "enEchec"        -> r.enEchec.map { case (mat, m, dec) =>
              JsObject(
                "matricule" -> JsString(mat),
                "moyenne"   -> JsNumber(BigDecimal(m).setScale(2, BigDecimal.RoundingMode.HALF_UP)),
                "decision"  -> JsString(DecisionAcademique.toString(dec))
              )
            }.toJson,
            "moyenneParMatiere"  -> r.moyenneParMatiere.toJson,
            "matiereLaPlusDifficile" -> r.matiereLaPlusDifficile.map { case (m, v) =>
              JsObject("matiere" -> JsString(m),
                       "moyenne" -> JsNumber(BigDecimal(v).setScale(2, BigDecimal.RoundingMode.HALF_UP)))
            }.getOrElse(JsNull),
            "tauxReussiteParFiliere" -> tauxFil.toJson,
            "filiereTopReussite"     -> topFil.map { case (f, t) =>
              JsObject("filiere" -> JsString(f),
                       "taux"    -> JsNumber(BigDecimal(t).setScale(4, BigDecimal.RoundingMode.HALF_UP)))
            }.getOrElse(JsNull)
          )
          res match {
            case Success(j) => complete(j)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("absenteisme") {
        get {
          service.indicateursAbsences() match {
            case Success(a) =>
              complete(JsObject(
                "totalHeuresNonJustifiees" -> JsNumber(a.totalHeures),
                "nbAbsencesNonJustifiees"  -> JsNumber(a.nonJustifiees),
                "enAlerte" -> a.enAlerte.map { case (m, h) =>
                  JsObject("matricule" -> JsString(m), "heures" -> JsNumber(h))
                }.toJson,
                "tauxParFiliere"   -> a.tauxParFiliere.toJson,
                "rapportParMatiere"-> a.rapportParMatiere.toJson
              ))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("financier") {
        get {
          service.indicateursFinanciers() match {
            case Success(f) =>
              complete(JsObject(
                "totalAttendu"     -> JsNumber(f.totalAttendu),
                "totalEncaisse"    -> JsNumber(f.totalEncaisse),
                "totalRestant"     -> JsNumber(f.totalRestant),
                "tauxRecouvrement" -> JsNumber(BigDecimal(f.tauxRecouvrement).setScale(4, BigDecimal.RoundingMode.HALF_UP)),
                "enDette" -> f.enDette.map { case (m, d) =>
                  JsObject("matricule" -> JsString(m), "dette" -> JsNumber(d))
                }.toJson,
                "parFiliere" -> f.parFiliere.map { case (fil, (du, paye, t)) =>
                  fil -> JsObject(
                    "attendu"  -> JsNumber(du),
                    "encaisse" -> JsNumber(paye),
                    "restant"  -> JsNumber(du - paye),
                    "taux"     -> JsNumber(BigDecimal(t).setScale(4, BigDecimal.RoundingMode.HALF_UP))
                  )
                }.toMap.toJson
              ))
            case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
          }
        }
      },

      path("risque") {
        get {
          parameters("seuil".as[Int].?(10)) { seuil =>
            service.etudiantsARisque(seuil) match {
              case Success(ls) => complete(ls.map { e =>
                JsObject(
                  "matricule"     -> JsString(e.matricule),
                  "nom"           -> JsString(e.nom),
                  "prenom"        -> JsString(e.prenom),
                  "filiere"       -> JsString(e.filiere),
                  "moyenne"       -> e.moyenne.map(m =>
                    JsNumber(BigDecimal(m).setScale(2, BigDecimal.RoundingMode.HALF_UP))
                  ).getOrElse(JsNull),
                  "heuresAbsence" -> JsNumber(e.heuresAbsence),
                  "motifs"        -> e.motifs.toJson
                )
              }.toJson)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        }
      },

      path("topEnseignants") {
        get {
          parameters("n".as[Int].?(5)) { n =>
            service.topEnseignantsParVolume(n) match {
              case Success(ls) => complete(ls.map { case (id, h) =>
                JsObject("idEnseignant" -> JsString(id), "heures" -> JsNumber(h))
              }.toJson)
              case Failure(ex) => complete(StatusCodes.InternalServerError, ErreurReponse(ex.getMessage))
            }
          }
        }
      }

    )
  }
}
