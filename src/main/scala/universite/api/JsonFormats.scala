package universite.api

import spray.json._
import universite.model._

import java.time.{LocalDate, LocalTime}
import java.time.format.DateTimeFormatter

/**
 * Formats spray-json pour serialiser/deserialiser les modeles metier.
 */
object JsonFormats extends DefaultJsonProtocol {

  // ---------- Types primitifs ----------
  implicit object LocalDateFormat extends RootJsonFormat[LocalDate] {
    override def write(d: LocalDate): JsValue = JsString(d.toString)
    override def read(v: JsValue): LocalDate = v match {
      case JsString(s) => LocalDate.parse(s)
      case _           => throw DeserializationException("Date attendue (YYYY-MM-DD)")
    }
  }

  implicit object LocalTimeFormat extends RootJsonFormat[LocalTime] {
    private val fmt = DateTimeFormatter.ofPattern("HH:mm")
    override def write(t: LocalTime): JsValue = JsString(t.format(fmt))
    override def read(v: JsValue): LocalTime = v match {
      case JsString(s) => LocalTime.parse(if (s.length == 5) s else s.substring(0, 5))
      case _           => throw DeserializationException("Heure attendue (HH:mm)")
    }
  }

  // ---------- Formats Map[String, _] manquants dans spray-json ----------
  // spray-json ne fournit pas ces formats automatiquement pour Map.
  implicit object MapStringIntFormat extends RootJsonFormat[Map[String, Int]] {
    override def write(m: Map[String, Int]): JsValue =
      JsObject(m.map { case (k, v) => k -> JsNumber(v) })
    override def read(v: JsValue): Map[String, Int] = v.asJsObject.fields.map {
      case (k, JsNumber(n)) => k -> n.toInt
      case (k, _)           => k -> 0
    }
  }
  implicit object MapStringDoubleFormat extends RootJsonFormat[Map[String, Double]] {
    override def write(m: Map[String, Double]): JsValue =
      JsObject(m.map { case (k, v) => k -> JsNumber(BigDecimal(v).setScale(4, BigDecimal.RoundingMode.HALF_UP)) })
    override def read(v: JsValue): Map[String, Double] = v.asJsObject.fields.map {
      case (k, JsNumber(n)) => k -> n.toDouble
      case (k, _)           => k -> 0.0
    }
  }
  implicit object MapStringStringFormat extends RootJsonFormat[Map[String, String]] {
    override def write(m: Map[String, String]): JsValue =
      JsObject(m.map { case (k, v) => k -> JsString(v) })
    override def read(v: JsValue): Map[String, String] = v.asJsObject.fields.map {
      case (k, JsString(s)) => k -> s
      case (k, other)       => k -> other.toString
    }
  }

  // ---------- Modeles de base ----------
  implicit val matiereFormat: RootJsonFormat[Matiere]       = jsonFormat6(Matiere)
  implicit val salleFormat:   RootJsonFormat[Salle]         = jsonFormat4(Salle)
  implicit val filiereFormat: RootJsonFormat[Filiere]       = jsonFormat3(Filiere)

  // Enseignant hérite de Personne → spray-json ne peut pas utiliser jsonFormat8
  // automatiquement sur une case class avec des champs hérités, on le fait manuellement.
  implicit object EnseignantFormat extends RootJsonFormat[Enseignant] {
    override def write(e: Enseignant): JsValue = JsObject(
      "idEnseignant" -> JsString(e.idEnseignant),
      "nom"          -> JsString(e.nom),
      "prenom"       -> JsString(e.prenom),
      "grade"        -> JsString(e.grade),
      "specialite"   -> JsString(e.specialite),
      "departement"  -> JsString(e.departement),
      "email"        -> JsString(e.email),
      "telephone"    -> JsString(e.telephone)
    )
    override def read(v: JsValue): Enseignant = {
      val o = v.asJsObject
      Enseignant(
        idEnseignant = o.fields("idEnseignant").convertTo[String],
        nom          = o.fields("nom").convertTo[String],
        prenom       = o.fields("prenom").convertTo[String],
        grade        = o.fields.get("grade").map(_.convertTo[String]).getOrElse(""),
        specialite   = o.fields.get("specialite").map(_.convertTo[String]).getOrElse(""),
        departement  = o.fields.get("departement").map(_.convertTo[String]).getOrElse(""),
        email        = o.fields.get("email").map(_.convertTo[String]).getOrElse(""),
        telephone    = o.fields.get("telephone").map(_.convertTo[String]).getOrElse("")
      )
    }
  }

  // ---------- Etudiant : encodage/decodage manuels (statut) ----------
  implicit object EtudiantFormat extends RootJsonFormat[Etudiant] {
    override def write(e: Etudiant): JsValue = JsObject(
      "matricule"     -> JsString(e.matricule),
      "nom"           -> JsString(e.nom),
      "prenom"        -> JsString(e.prenom),
      "sexe"          -> JsString(e.sexe),
      "dateNaissance" -> e.dateNaissance.map(_.toJson).getOrElse(JsNull),
      "email"         -> JsString(e.email),
      "telephone"     -> JsString(e.telephone),
      "filiere"       -> JsString(e.filiere),
      "niveau"        -> JsString(e.niveau),
      "annee"         -> JsString(e.annee),
      "statut"        -> JsString(StatutEtudiant.toString(e.statut))
    )
    override def read(v: JsValue): Etudiant = {
      val o = v.asJsObject
      Etudiant(
        matricule     = o.fields("matricule").convertTo[String],
        nom           = o.fields("nom").convertTo[String],
        prenom        = o.fields("prenom").convertTo[String],
        sexe          = o.fields.get("sexe").map(_.convertTo[String]).getOrElse(""),
        dateNaissance = o.fields.get("dateNaissance").flatMap {
          case JsNull      => None
          case JsString(s) => Some(LocalDate.parse(s))
          case _           => None
        },
        email         = o.fields.get("email").map(_.convertTo[String]).getOrElse(""),
        telephone     = o.fields.get("telephone").map(_.convertTo[String]).getOrElse(""),
        filiere       = o.fields.get("filiere").map(_.convertTo[String]).getOrElse(""),
        niveau        = o.fields.get("niveau").map(_.convertTo[String]).getOrElse(""),
        annee         = o.fields.get("annee").map(_.convertTo[String]).getOrElse(""),
        statut        = o.fields.get("statut") match {
          case Some(JsString(s)) => StatutEtudiant.fromString(s)
          case _                 => StatutEtudiant.Inconnu
        }
      )
    }
  }

  // ---------- Note ----------
  implicit object NoteFormat extends RootJsonFormat[Note] {
    override def write(n: Note): JsValue = JsObject(
      "idNote"          -> JsString(n.idNote),
      "matricule"       -> JsString(n.matricule),
      "matiere"         -> JsString(n.matiere),
      "controleContinu" -> n.controleContinu.map(JsNumber(_)).getOrElse(JsNull),
      "examen"          -> n.examen.map(JsNumber(_)).getOrElse(JsNull),
      "moyenne"         -> n.moyenneOption.map(m => JsNumber(BigDecimal(m).setScale(2, BigDecimal.RoundingMode.HALF_UP))).getOrElse(JsNull),
      "valide"          -> JsBoolean(n.estValide),
      "complete"        -> JsBoolean(n.estComplete)
    )
    override def read(v: JsValue): Note = {
      val o = v.asJsObject
      Note(
        idNote          = o.fields.get("idNote").collect { case JsString(s) => s }.getOrElse(""),
        matricule       = o.fields("matricule").convertTo[String],
        matiere         = o.fields("matiere").convertTo[String],
        controleContinu = o.fields.get("controleContinu") match {
          case Some(JsNumber(b)) => Some(b.toDouble); case _ => None
        },
        examen          = o.fields.get("examen") match {
          case Some(JsNumber(b)) => Some(b.toDouble); case _ => None
        }
      )
    }
  }

  // ---------- Absence ----------
  implicit val absenceFormat: RootJsonFormat[Absence] = jsonFormat6(Absence)

  // ---------- SeanceCours ----------
  implicit val seanceFormat: RootJsonFormat[SeanceCours] = jsonFormat9(SeanceCours)

  // ---------- Enveloppes API ----------
  case class ErreurReponse(erreur: String, details: Option[String] = None)
  implicit val erreurFormat: RootJsonFormat[ErreurReponse] = jsonFormat2(ErreurReponse)

  case class SuccesReponse(message: String)
  implicit val succesFormat: RootJsonFormat[SuccesReponse] = jsonFormat1(SuccesReponse)

  // Conflits emploi du temps
  case class ConflitInfo(seanceA: SeanceCours, seanceB: SeanceCours, raison: String)
  implicit val conflitFormat: RootJsonFormat[ConflitInfo] = jsonFormat3(ConflitInfo)

  // ---------- Inscription ----------
  implicit object InscriptionFormat extends RootJsonFormat[Inscription] {
    override def write(i: Inscription): JsValue = JsObject(
      "idInscription" -> JsString(i.idInscription),
      "matricule"     -> JsString(i.matricule),
      "filiere"       -> JsString(i.filiere),
      "niveau"        -> JsString(i.niveau),
      "annee"         -> JsString(i.annee),
      "statut"        -> JsString(StatutInscription.toString(i.statut))
    )
    override def read(v: JsValue): Inscription = {
      val o = v.asJsObject
      Inscription(
        idInscription = o.fields.get("idInscription").collect { case JsString(s) => s }.getOrElse(""),
        matricule     = o.fields("matricule").convertTo[String],
        filiere       = o.fields("filiere").convertTo[String],
        niveau        = o.fields("niveau").convertTo[String],
        annee         = o.fields("annee").convertTo[String],
        statut        = o.fields.get("statut") match {
          case Some(JsString(s)) => StatutInscription.fromString(s)
          case _                 => StatutInscription.EnAttente
        }
      )
    }
  }

  // ---------- Paiement ----------
  implicit object PaiementFormat extends RootJsonFormat[Paiement] {
    override def write(p: Paiement): JsValue = JsObject(
      "idPaiement"    -> JsString(p.idPaiement),
      "matricule"     -> JsString(p.matricule),
      "montantTotal"  -> JsNumber(p.montantTotal),
      "montantPaye"   -> JsNumber(p.montantPaye),
      "datePaiement"  -> p.datePaiement.map(_.toJson).getOrElse(JsNull),
      "mode"          -> JsString(ModePaiement.toString(p.mode)),
      "reste"         -> JsNumber(p.reste),
      "tauxPaiement"  -> JsNumber(BigDecimal(p.tauxPaiement).setScale(4, BigDecimal.RoundingMode.HALF_UP)),
      "estSolde"      -> JsBoolean(p.estSolde)
    )
    override def read(v: JsValue): Paiement = {
      val o = v.asJsObject
      Paiement(
        idPaiement   = o.fields.get("idPaiement").collect { case JsString(s) => s }.getOrElse(""),
        matricule    = o.fields("matricule").convertTo[String],
        montantTotal = o.fields("montantTotal").convertTo[Double],
        montantPaye  = o.fields("montantPaye").convertTo[Double],
        datePaiement = o.fields.get("datePaiement").flatMap {
          case JsNull       => None
          case JsString(s)  => Some(LocalDate.parse(s))
          case _            => None
        },
        mode         = o.fields.get("mode") match {
          case Some(JsString(s)) => ModePaiement.fromString(s)
          case _                 => ModePaiement.Inconnu
        }
      )
    }
  }
}