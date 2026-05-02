package universite.model

// ─────────────────────────────────────────────
//  Module 4 : Modèle Inscription
// ─────────────────────────────────────────────

sealed trait StatutInscription
case object Validee   extends StatutInscription
case object EnAttente extends StatutInscription
case object Annulee   extends StatutInscription

object StatutInscription {
  def fromString(s: String): StatutInscription = s.trim.toLowerCase match {
    case "validee"    | "validée"    => Validee
    case "en attente"                => EnAttente
    case "annulee"    | "annulée"    => Annulee
    case autre => throw new IllegalArgumentException(s"Statut inconnu : $autre")
  }
}

case class Inscription(
  idInscription  : String,
  matricule      : String,
  filiere        : String,
  niveau         : String,
  anneeAcademique: String,
  statut         : StatutInscription
) extends Identifiable with Affichable with Validable {

  override def id: String = idInscription

  override def afficher(): Unit =
    println(f"[$idInscription] $matricule | $filiere%-15s $niveau | $anneeAcademique | $statut")

  override def estValide: Boolean =
    idInscription.nonEmpty && matricule.nonEmpty && filiere.nonEmpty

  def estActive: Boolean = statut == Validee
}

object Inscription {
  def fromCsv(ligne: String): Option[Inscription] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 6) None
    else scala.util.Try(
      Inscription(cols(0), cols(1), cols(2), cols(3), cols(4), StatutInscription.fromString(cols(5)))
    ).toOption
  }
}
