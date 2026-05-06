package universite.model

/**
 * Statuts possibles d'une inscription (sealed pour pattern matching exhaustif).
 */
sealed trait StatutInscription
object StatutInscription {
  case object Validee   extends StatutInscription
  case object EnAttente extends StatutInscription
  case object Annulee   extends StatutInscription
  case object Inconnu   extends StatutInscription

  def fromString(s: String): StatutInscription = s.trim.toLowerCase match {
    case "validee" | "validée" | "valide" => Validee
    case "en attente" | "attente"          => EnAttente
    case "annulee" | "annulée" | "annule"  => Annulee
    case _                                  => Inconnu
  }

  def toString(s: StatutInscription): String = s match {
    case Validee   => "Validee"
    case EnAttente => "En attente"
    case Annulee   => "Annulee"
    case Inconnu   => "Inconnu"
  }
}

/**
 * Module 4 : Inscription d'un etudiant a une annee academique.
 * Un meme etudiant ne peut etre inscrit qu'une fois par annee (UNIQUE en BDD).
 */
case class Inscription(
  idInscription: String,
  matricule:     String,
  filiere:       String,
  niveau:        String,
  annee:         String,
  statut:        StatutInscription
) extends Identifiable with Affichable with Validable {

  override def id: String = idInscription

  override def afficher: String =
    s"[$idInscription] $matricule - $filiere $niveau ($annee) : ${StatutInscription.toString(statut)}"

  override def estValide: Boolean =
    matricule.nonEmpty && filiere.nonEmpty && niveau.nonEmpty && annee.matches("\\d{4}-\\d{4}")
}
