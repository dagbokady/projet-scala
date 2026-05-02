package universite.model

// ─────────────────────────────────────────────
//  Module 1 : Modèle Étudiant
// ─────────────────────────────────────────────

sealed trait StatutEtudiant
case object Actif    extends StatutEtudiant
case object Suspendu extends StatutEtudiant
case object Diplome  extends StatutEtudiant

object StatutEtudiant {
  def fromString(s: String): StatutEtudiant = s.trim.toLowerCase match {
    case "actif"    => Actif
    case "suspendu" => Suspendu
    case "diplome"  => Diplome
    case autre      => throw new IllegalArgumentException(s"Statut inconnu : $autre")
  }
}

case class Etudiant(
  matricule      : String,
  nom            : String,
  prenom         : String,
  sexe           : String,
  dateNaissance  : String,
  email          : String,
  telephone      : String,
  filiere        : String,
  niveau         : String,
  anneeAcademique: String,
  statut         : StatutEtudiant
) extends Identifiable with Affichable with Validable {

  override def id: String = matricule

  override def afficher(): Unit =
    println(f"[$matricule] $nom%-12s $prenom%-12s | $filiere%-15s $niveau | $statut")

  override def estValide: Boolean =
    matricule.nonEmpty && email.nonEmpty && nom.nonEmpty

  def nomComplet: String = s"$prenom $nom"
}

object Etudiant {
  def fromCsv(ligne: String): Option[Etudiant] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 11) None
    else scala.util.Try(
      Etudiant(
        matricule       = cols(0),
        nom             = cols(1),
        prenom          = cols(2),
        sexe            = cols(3),
        dateNaissance   = cols(4),
        email           = cols(5),
        telephone       = cols(6),
        filiere         = cols(7),
        niveau          = cols(8),
        anneeAcademique = cols(9),
        statut          = StatutEtudiant.fromString(cols(10))
      )
    ).toOption
  }
}
