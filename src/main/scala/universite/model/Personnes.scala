package universite.model

import java.time.LocalDate

/**
 * Classe abstraite Personne : utilisee pour l'heritage exige par le projet.
 * Etudiant et Enseignant en heritent.
 */
abstract class Personne(
  val nom: String,
  val prenom: String,
  val email: String,
  val telephone: String
) extends Identifiable with Affichable {
  def nomComplet: String = s"$prenom $nom"
}

/** Statuts possibles d'un etudiant (sealed pour pattern matching exhaustif). */
sealed trait StatutEtudiant
object StatutEtudiant {
  case object Actif     extends StatutEtudiant
  case object Suspendu  extends StatutEtudiant
  case object Diplome   extends StatutEtudiant
  case object Inconnu   extends StatutEtudiant

  def fromString(s: String): StatutEtudiant = s.trim.toLowerCase match {
    case "actif"     => Actif
    case "suspendu"  => Suspendu
    case "diplome" | "diplôme" | "diplomé" => Diplome
    case _           => Inconnu
  }

  def toString(st: StatutEtudiant): String = st match {
    case Actif    => "Actif"
    case Suspendu => "Suspendu"
    case Diplome  => "Diplome"
    case Inconnu  => "Inconnu"
  }
}

case class Etudiant(
  matricule:     String,
  override val nom: String,
  override val prenom: String,
  sexe:          String,
  dateNaissance: Option[LocalDate],
  override val email: String,
  override val telephone: String,
  filiere:       String,
  niveau:        String,
  annee:         String,
  statut:        StatutEtudiant
) extends Personne(nom, prenom, email, telephone) with Recherchable[String] {

  override def id: String = matricule

  override def afficher: String =
    f"[$matricule%-8s] $nomComplet%-30s | $filiere%-15s | $niveau%-3s | ${StatutEtudiant.toString(statut)}"

  /** Recherche par matricule, nom, prenom ou email. */
  override def correspondA(critere: String): Boolean = {
    val c = critere.trim.toLowerCase
    matricule.toLowerCase.contains(c) ||
    nom.toLowerCase.contains(c) ||
    prenom.toLowerCase.contains(c) ||
    email.toLowerCase.contains(c)
  }
}

case class Enseignant(
  idEnseignant: String,
  override val nom: String,
  override val prenom: String,
  grade:        String,
  specialite:   String,
  departement:  String,
  override val email: String,
  override val telephone: String
) extends Personne(nom, prenom, email, telephone) {
  override def id: String = idEnseignant
  override def afficher: String =
    s"[$idEnseignant] $nomComplet ($grade, $specialite) - Dept: $departement"
}
