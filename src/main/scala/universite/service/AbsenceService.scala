package universite.service

import universite.model.Absence
import universite.repository.{AbsenceRepository, EtudiantRepository}

import scala.annotation.tailrec
import scala.util.Try

/**
 * Service du Module 6 : Gestion des absences.
 *
 * Regles metier :
 *   - une absence non justifiee compte dans le total
 *   - seuil d'alerte par defaut : 10 heures cumulees
 *   - taux d'absenteisme par filiere
 */
class AbsenceService(
  absenceRepo:  AbsenceRepository  = new AbsenceRepository,
  etudiantRepo: EtudiantRepository = new EtudiantRepository
) {

  val seuilAlerte: Int = 10

  // ---------------------------------------------------------------
  // Lecture
  // ---------------------------------------------------------------

  def listerToutes(): Try[List[Absence]]                = absenceRepo.listerToutes()
  def listerParEtudiant(m: String): Try[List[Absence]]  = absenceRepo.listerParEtudiant(m)
  def listerParMatiere(m: String): Try[List[Absence]]   = absenceRepo.listerParMatiere(m)
  def trouverParId(id: String): Try[Option[Absence]]    = absenceRepo.trouverParId(id)

  // ---------------------------------------------------------------
  // Saisie / modification
  // ---------------------------------------------------------------

  def enregistrer(a: Absence): Try[Absence] = {
    val avecId =
      if (a.idAbsence.isEmpty || a.idAbsence.startsWith("auto"))
        a.copy(idAbsence = absenceRepo.prochainId().getOrElse("ABS999"))
      else a

    if (!avecId.estValide)
      Try(throw new IllegalArgumentException(
        s"Absence invalide : nombre d'heures doit etre > 0"
      ))
    else
      absenceRepo.enregistrer(avecId)
  }

  /** Marque une absence comme justifiee ou non. */
  def justifier(id: String, justifiee: Boolean): Try[Int] =
    absenceRepo.justifier(id, justifiee)

  def supprimer(id: String): Try[Int] = absenceRepo.supprimer(id)

  // ---------------------------------------------------------------
  // Calculs (recursivite + fonctions d'ordre superieur)
  // ---------------------------------------------------------------

  /** Recursion exigee : somme des heures d'absence (terminale). */
  @tailrec
  final def totalHeuresRecursive(absences: List[Absence], acc: Int = 0): Int =
    absences match {
      case Nil          => acc
      case head :: tail => totalHeuresRecursive(tail, acc + head.heures)
    }

  /**
   * Total d'heures d'absence pour un etudiant.
   * Par defaut on compte uniquement les non justifiees (regle metier classique).
   */
  def totalHeures(matricule: String, inclureJustifiees: Boolean = false): Try[Int] =
    absenceRepo.listerParEtudiant(matricule).map { abs =>
      val pertinentes = if (inclureJustifiees) abs else abs.filterNot(_.justifiee)
      totalHeuresRecursive(pertinentes)
    }

  /** Liste des absences non justifiees (filter). */
  def absencesNonJustifiees(): Try[List[Absence]] =
    absenceRepo.listerToutes().map(_.filterNot(_.justifiee))

  /**
   * Etudiants ayant depasse le seuil d'absences (10h par defaut).
   * Renvoie une liste (matricule, total_heures) triee.
   */
  def etudiantsEnAlerte(seuil: Int = seuilAlerte): Try[List[(String, Int)]] =
    absenceRepo.listerToutes().map { all =>
      all
        .filterNot(_.justifiee)
        .groupBy(_.matricule)
        .view
        .mapValues(lst => totalHeuresRecursive(lst))
        .filter { case (_, total) => total >= seuil }
        .toList
        .sortBy { case (_, total) => -total }
    }

  /**
   * Taux d'absenteisme par filiere = (heures absences non justifiees) / (etudiants * volume horaire moyen).
   * On utilise une formule simplifiee : moyenne des heures d'absence par etudiant de la filiere.
   */
  def tauxParFiliere(): Try[Map[String, Double]] =
    for {
      etus <- etudiantRepo.listerTous()
      abss <- absenceRepo.listerToutes()
    } yield {
      val parFil: Map[String, List[String]] = etus.groupBy(_.filiere).view.mapValues(_.map(_.matricule)).toMap
      val absNonJust = abss.filterNot(_.justifiee)
      parFil.map { case (fil, mats) =>
        val totalHeures = absNonJust.filter(a => mats.contains(a.matricule)).map(_.heures).sum
        val moyenne = if (mats.isEmpty) 0.0 else totalHeures.toDouble / mats.size
        fil -> moyenne
      }
    }

  /** Rapport des absences par matiere. */
  def rapportParMatiere(): Try[Map[String, Int]] =
    absenceRepo.listerToutes().map { abs =>
      abs
        .filterNot(_.justifiee)
        .groupBy(_.matiere)
        .view
        .mapValues(lst => totalHeuresRecursive(lst))
        .toMap
    }
}
