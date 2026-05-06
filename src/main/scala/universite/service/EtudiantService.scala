package universite.service

import universite.model._
import universite.repository.EtudiantRepository

import scala.annotation.tailrec
import scala.util.Try

/**
 * Service du Module 1 : Gestion des etudiants.
 *
 * Couvre :
 *  - CRUD complet
 *  - filtrage par filiere / niveau / statut
 *  - comptages (actifs, suspendus, diplomes)
 *  - recherche recursive par matricule
 */
class EtudiantService(repo: EtudiantRepository = new EtudiantRepository) {

  // ---------- Lecture ----------
  def listerTous(): Try[List[Etudiant]]                    = repo.listerTous()
  def trouverParMatricule(m: String): Try[Option[Etudiant]] = repo.trouverParMatricule(m)

  def filtrerParFiliere(filiere: String): Try[List[Etudiant]] = repo.listerParFiliere(filiere)
  def filtrerParNiveau(niveau: String):  Try[List[Etudiant]]  = repo.listerParNiveau(niveau)
  def filtrerParStatut(statut: String):  Try[List[Etudiant]]  = repo.listerParStatut(statut)

  // ---------- Recursivite ----------
  /**
   * Recherche recursive d'un etudiant par matricule dans une liste.
   * Demonstration de pattern matching exigee par le PDF.
   */
  @tailrec
  final def chercherRecursif(matricule: String, liste: List[Etudiant]): Option[Etudiant] =
    liste match {
      case Nil                                       => None
      case head :: _ if head.matricule == matricule => Some(head)
      case _ :: tail                                 => chercherRecursif(matricule, tail)
    }

  /** Comptage recursif d'etudiants actifs. */
  @tailrec
  final def compterActifsRecursif(liste: List[Etudiant], acc: Int = 0): Int = liste match {
    case Nil                                                  => acc
    case head :: tail if head.statut == StatutEtudiant.Actif => compterActifsRecursif(tail, acc + 1)
    case _    :: tail                                         => compterActifsRecursif(tail, acc)
  }

  // ---------- Indicateurs ----------
  def compterActifs(): Try[Int] =
    repo.listerTous().map(compterActifsRecursif(_))

  def compterSuspendus(): Try[Int] =
    repo.listerTous().map(_.count(_.statut == StatutEtudiant.Suspendu))

  def compterDiplomes(): Try[Int] =
    repo.listerTous().map(_.count(_.statut == StatutEtudiant.Diplome))

  /** Compter les etudiants par filiere (demo de fonction d'ordre superieur). */
  def comptageParFiliere(): Try[Map[String, Int]] =
    repo.listerTous().map { etus =>
      etus.groupBy(_.filiere).view.mapValues(_.size).toMap
    }

  /** Compter les etudiants par niveau. */
  def comptageParNiveau(): Try[Map[String, Int]] =
    repo.listerTous().map { etus =>
      etus.groupBy(_.niveau).view.mapValues(_.size).toMap
    }

  /** Set des filieres uniques (Set comme exige par le PDF). */
  def filieresUniques(): Try[Set[String]] =
    repo.listerTous().map(_.map(_.filiere).filter(_.nonEmpty).toSet)

  // ---------- Ecriture ----------
  def enregistrer(e: Etudiant): Try[Etudiant] = {
    val avec =
      if (e.matricule.isEmpty || e.matricule.startsWith("auto"))
        e.copy(matricule = repo.prochainMatricule().getOrElse("ETU999"))
      else e
    repo.enregistrer(avec)
  }

  def changerStatut(matricule: String, nouveau: StatutEtudiant): Try[Etudiant] =
    repo.trouverParMatricule(matricule).flatMap {
      case Some(e) => repo.enregistrer(e.copy(statut = nouveau))
      case None    => Try(throw new NoSuchElementException(s"Etudiant $matricule introuvable"))
    }

  def supprimer(matricule: String): Try[Int] = repo.supprimer(matricule)
}
