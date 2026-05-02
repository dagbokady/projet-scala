package universite.model

// ─────────────────────────────────────────────
//  Module 2 : Modèle Enseignant
// ─────────────────────────────────────────────

case class Enseignant(
  idEnseignant : String,
  nom          : String,
  prenom       : String,
  grade        : String,
  specialite   : String,
  departement  : String,
  email        : String,
  telephone    : String,
  coursAffecter: List[String] = List.empty   // ids des matières affectées
) extends Identifiable with Affichable with Validable {

  override def id: String = idEnseignant

  override def afficher(): Unit =
    println(f"[$idEnseignant] $nom%-12s $prenom%-12s | $grade%-20s | Dept: $departement")

  override def estValide: Boolean =
    idEnseignant.nonEmpty && email.nonEmpty && nom.nonEmpty

  def nomComplet: String = s"$prenom $nom"

  /** Retourne une copie avec le cours ajouté (immutabilité) */
  def affecterCours(idMatiere: String): Enseignant =
    this.copy(coursAffecter = (idMatiere :: coursAffecter).distinct)
}

object Enseignant {
  def fromCsv(ligne: String): Option[Enseignant] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 8) None
    else scala.util.Try(
      Enseignant(
        idEnseignant = cols(0),
        nom          = cols(1),
        prenom       = cols(2),
        grade        = cols(3),
        specialite   = cols(4),
        departement  = cols(5),
        email        = cols(6),
        telephone    = cols(7)
      )
    ).toOption
  }
}
