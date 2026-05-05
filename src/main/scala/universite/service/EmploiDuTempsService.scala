package universite.service

import universite.model.SeanceCours
import universite.repository.SeanceRepository

import scala.util.{Failure, Success, Try}

/**
 * Service du Module 7 : Gestion des emplois du temps.
 *
 * Regles :
 *   - une seance doit avoir heure_fin > heure_debut
 *   - aucun chevauchement d'horaire pour une meme salle
 *   - aucun chevauchement d'horaire pour un meme enseignant
 *   - aucun chevauchement pour une meme classe (filiere + niveau)
 */
class EmploiDuTempsService(
  seanceRepo: SeanceRepository = new SeanceRepository
) {

  // ---------------------------------------------------------------
  // Lecture
  // ---------------------------------------------------------------

  def listerToutes(): Try[List[SeanceCours]] = seanceRepo.listerToutes()

  def emploiDeClasse(filiere: String, niveau: String): Try[List[SeanceCours]] =
    seanceRepo.listerParFiliereEtNiveau(filiere, niveau)

  def emploiEnseignant(idEnseignant: String): Try[List[SeanceCours]] =
    seanceRepo.listerParEnseignant(idEnseignant)

  def emploiSalle(idSalle: String): Try[List[SeanceCours]] =
    seanceRepo.listerParSalle(idSalle)

  // ---------------------------------------------------------------
  // Detection de conflits
  // ---------------------------------------------------------------

  /**
   * Detecte les conflits entre une nouvelle seance et celles existantes.
   * Renvoie la liste des conflits trouves (vide si pas de conflit).
   */
  def detecterConflitsAvec(nouvelle: SeanceCours): Try[List[(SeanceCours, String)]] =
    seanceRepo.listerToutes().map { existantes =>
      existantes
        .filter(_.idSeance != nouvelle.idSeance)
        .flatMap { autre =>
          if (!nouvelle.chevaucheAvec(autre)) Nil
          else {
            val raisons =
              (if (autre.salle == nouvelle.salle)            List("salle")           else Nil) ++
              (if (autre.enseignant == nouvelle.enseignant)  List("enseignant")      else Nil) ++
              (if (autre.filiere == nouvelle.filiere && autre.niveau == nouvelle.niveau)
                List("classe")
               else Nil)
            if (raisons.isEmpty) Nil
            else List((autre, raisons.mkString(", ")))
          }
        }
    }

  /** Liste tous les conflits internes a l'emploi du temps actuel. */
  def detecterTousConflits(): Try[List[(SeanceCours, SeanceCours, String)]] =
    seanceRepo.listerToutes().map { ss =>
      val pairs = for {
        i <- ss.indices.toList
        j <- (i + 1 until ss.size).toList
        a = ss(i)
        b = ss(j)
        if a.chevaucheAvec(b)
      } yield {
        val raisons =
          (if (a.salle == b.salle)           List("meme salle")      else Nil) ++
          (if (a.enseignant == b.enseignant) List("meme enseignant") else Nil) ++
          (if (a.filiere == b.filiere && a.niveau == b.niveau)
            List("meme classe")
           else Nil)
        if (raisons.nonEmpty) Some((a, b, raisons.mkString(", "))) else None
      }
      pairs.flatten
    }

  // ---------------------------------------------------------------
  // Creation / modification avec validation
  // ---------------------------------------------------------------

  /**
   * Cree une seance apres avoir verifie qu'elle ne genere pas de conflit.
   * Retourne Failure si un conflit est detecte.
   */
  def creer(nouvelle: SeanceCours): Try[SeanceCours] = {
    val avecId =
      if (nouvelle.idSeance.isEmpty || nouvelle.idSeance.startsWith("auto"))
        nouvelle.copy(idSeance = seanceRepo.prochainId().getOrElse("SEA999"))
      else nouvelle

    if (!avecId.estValide)
      return Failure(new IllegalArgumentException(
        "Seance invalide : l'heure de fin doit etre apres l'heure de debut"
      ))

    detecterConflitsAvec(avecId) match {
      case Success(Nil) =>
        seanceRepo.enregistrer(avecId)
      case Success(conflits) =>
        val msg = conflits
          .map { case (s, raison) => s"${s.idSeance} ($raison)" }
          .mkString(", ")
        Failure(new IllegalStateException(s"Conflit detecte avec : $msg"))
      case Failure(ex) => Failure(ex)
    }
  }

  /** Modifie une seance avec verification de conflits. */
  def modifier(s: SeanceCours): Try[SeanceCours] = creer(s)

  def supprimer(id: String): Try[Int] = seanceRepo.supprimer(id)
}
