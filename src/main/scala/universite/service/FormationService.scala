package universite.service

import universite.model._
import universite.repository.{FiliereRepository, MatiereRepository}

import scala.util.Try

/**
 * Service du Module 3 : Gestion des formations (filieres, matieres, UE).
 *
 * Hierarchie : Filiere -> [Niveau] -> [Semestre] -> UE -> Matieres
 * (Le projet utilise une version simplifiee : Matiere porte directement l'UE.)
 */
class FormationService(
  filiereRepo: FiliereRepository = new FiliereRepository,
  matiereRepo: MatiereRepository = new MatiereRepository
) {

  // ---------- Filieres ----------
  def listerFilieres(): Try[List[Filiere]]                = filiereRepo.listerToutes()
  def trouverFiliere(id: String): Try[Option[Filiere]]    = filiereRepo.trouverParId(id)
  def enregistrerFiliere(f: Filiere): Try[Filiere]        = filiereRepo.enregistrer(f)
  def supprimerFiliere(id: String): Try[Int]              = filiereRepo.supprimer(id)

  // ---------- Matieres ----------
  def listerMatieres(): Try[List[Matiere]]                = matiereRepo.listerToutes()
  def trouverMatiere(id: String): Try[Option[Matiere]]    = matiereRepo.trouverParId(id)
  def enregistrerMatiere(m: Matiere): Try[Matiere]        = matiereRepo.enregistrer(m)
  def supprimerMatiere(id: String): Try[Int]              = matiereRepo.supprimer(id)

  // ---------- UE ----------
  /**
   * Regroupement des matieres par UE.
   * Demonstration de groupBy + Map[String, List[Matiere]].
   */
  def matieresParUe(): Try[Map[String, List[Matiere]]] =
    matiereRepo.listerToutes().map(_.groupBy(_.ue))

  /** Volume horaire total par UE (foldLeft). */
  def volumeHoraireParUe(): Try[Map[String, Int]] =
    matiereRepo.listerToutes().map { mats =>
      mats.groupBy(_.ue).view.mapValues { lst =>
        lst.foldLeft(0)((acc, m) => acc + m.volumeHoraire)
      }.toMap
    }

  /** Coefficient cumule par UE. */
  def coefficientParUe(): Try[Map[String, Int]] =
    matiereRepo.listerToutes().map { mats =>
      mats.groupBy(_.ue).view.mapValues { lst =>
        lst.foldLeft(0)((acc, m) => acc + m.coefficient)
      }.toMap
    }

  /** Liste des UE uniques (Set). */
  def uesUniques(): Try[Set[String]] =
    matiereRepo.listerToutes().map(_.map(_.ue).filter(_.nonEmpty).toSet)
}
