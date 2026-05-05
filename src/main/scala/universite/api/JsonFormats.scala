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

  // ---------- Modeles de base ----------
  implicit val matiereFormat: RootJsonFormat[Matiere]       = jsonFormat6(Matiere)
  implicit val salleFormat:   RootJsonFormat[Salle]         = jsonFormat4(Salle)

  implicit val enseignantFormat: RootJsonFormat[Enseignant] = jsonFormat(
    Enseignant,
    "idEnseignant",
    "nom",
    "prenom",
    "grade",
    "specialite",
    "departement",
    "email",
    "telephone"
  )

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
}
