package universite.service

import universite.model._
import universite.repository.{EnseignantRepository, MatiereRepository}

import scala.util.Try

/**
 * Service du Module 2 : Gestion des enseignants.
 *
 * Couvre :
 *  - CRUD
 *  - filtrage par departement
 *  - cours assures par un enseignant (jointure avec Matiere)
 */
class EnseignantService(
  ensRepo: EnseignantRepository = new EnseignantRepository,
  matRepo: MatiereRepository    = new MatiereRepository
) {

  def listerTous(): Try[List[Enseignant]]                          = ensRepo.listerTous()
  def trouverParId(id: String): Try[Option[Enseignant]]            = ensRepo.trouverParId(id)
  def filtrerParDepartement(d: String): Try[List[Enseignant]]      = ensRepo.listerParDepartement(d)

  /** Cours assures par un enseignant (jointure metier). */
  def coursAssures(idEnseignant: String): Try[List[Matiere]] =
    matRepo.listerParEnseignant(idEnseignant)

  /** Volume horaire total assure par chaque enseignant (foldLeft). */
  def volumeHoraireParEnseignant(): Try[Map[String, Int]] =
    matRepo.listerToutes().map { mats =>
      mats.groupBy(_.enseignant).view.mapValues { lst =>
        // foldLeft exige par le projet
        lst.foldLeft(0)((acc, m) => acc + m.volumeHoraire)
      }.toMap
    }

  /** Tri des enseignants par volume horaire decroissant (Module 9 : top enseignants). */
  def topVolumeHoraire(n: Int = 5): Try[List[(String, Int)]] =
    volumeHoraireParEnseignant().map(_.toList.sortBy(-_._2).take(n))

  def comptageParDepartement(): Try[Map[String, Int]] =
    ensRepo.listerTous().map { ens =>
      ens.groupBy(_.departement).view.mapValues(_.size).toMap
    }

  def enregistrer(e: Enseignant): Try[Enseignant] = ensRepo.enregistrer(e)
  def supprimer(id: String): Try[Int]              = ensRepo.supprimer(id)
}
