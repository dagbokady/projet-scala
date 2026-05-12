package universite.model

import java.time.LocalTime

/**
 * Seance de cours dans l'emploi du temps.
 *
 * Une seance est consideree en conflit avec une autre lorsque deux seances
 * partagent la meme ressource (salle ou enseignant) le meme jour avec des
 * plages horaires qui se chevauchent.
 */
case class SeanceCours(
  idSeance:    String = "",
  matiere:     String,
  enseignant:  String,
  salle:       String,
  jour:        String,
  heureDebut:  LocalTime,
  heureFin:    LocalTime,
  filiere:     String,
  niveau:      String
) extends Identifiable with Validable with Affichable {

  override def id: String = idSeance

  /** Une seance est valide si l'heure de fin est strictement apres le debut. */
  override def estValide: Boolean = heureFin.isAfter(heureDebut)

  /** Verifie si deux seances se chevauchent dans le temps (memes jour). */
  def chevaucheAvec(autre: SeanceCours): Boolean = {
    if (jour != autre.jour) false
    else heureDebut.isBefore(autre.heureFin) && autre.heureDebut.isBefore(heureFin)
  }

  /** Conflit reel = chevauchement + meme salle OU meme enseignant. */
  def enConflitAvec(autre: SeanceCours): Boolean = {
    if (idSeance == autre.idSeance) false
    else chevaucheAvec(autre) && (salle == autre.salle || enseignant == autre.enseignant)
  }

  override def afficher: String =
    s"[$idSeance] $jour $heureDebut-$heureFin | $matiere | salle $salle | ens. $enseignant | $filiere $niveau"
}
