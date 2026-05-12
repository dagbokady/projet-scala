package universite.model

import java.time.LocalDate

/**
 * Absence d'un etudiant a une seance de cours pour une matiere donnee.
 */
case class Absence(
                    idAbsence:   String = "",
                    matricule:   String,
                    matiere:     String,
                    dateAbsence: LocalDate,
                    heures:      Int,
                    justifiee:   Boolean = false
                  ) extends Identifiable with Validable with Affichable {

  override def id: String = idAbsence

  /** Une absence est valide si elle a un nombre d'heures > 0. */
  override def estValide: Boolean = heures > 0

  override def afficher: String = {
    val just = if (justifiee) "justifiee" else "non justifiee"
    s"[$idAbsence] $matricule / $matiere - $dateAbsence : ${heures}h ($just)"
  }
}
