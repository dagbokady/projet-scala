package universite.service

import universite.model._
import universite.repository.{EtudiantRepository, InscriptionRepository}

import scala.util.{Failure, Try}

/**
 * Service du Module 4 : Gestion des inscriptions.
 *
 * Regles metier :
 *  - un etudiant ne peut etre inscrit qu'une fois par annee academique
 *  - statuts : Validee / En attente / Annulee
 *  - une inscription validee positionne l'etudiant en filiere/niveau correspondants
 */
class InscriptionService(
  inscRepo: InscriptionRepository = new InscriptionRepository,
  etuRepo:  EtudiantRepository    = new EtudiantRepository
) {

  // ---------- Lecture ----------
  def listerToutes(): Try[List[Inscription]]                       = inscRepo.listerToutes()
  def trouverParId(id: String): Try[Option[Inscription]]           = inscRepo.trouverParId(id)
  def listerParEtudiant(matricule: String): Try[List[Inscription]] = inscRepo.listerParEtudiant(matricule)

  def listerValidees(): Try[List[Inscription]] =
    inscRepo.listerToutes().map(_.filter(_.statut == StatutInscription.Validee))

  def listerEnAttente(): Try[List[Inscription]] =
    inscRepo.listerToutes().map(_.filter(_.statut == StatutInscription.EnAttente))

  def listerAnnulees(): Try[List[Inscription]] =
    inscRepo.listerToutes().map(_.filter(_.statut == StatutInscription.Annulee))

  // ---------- Ecriture avec validations metier ----------

  /**
   * Inscrit un etudiant a une annee academique apres verification :
   *   - l'etudiant existe
   *   - il n'est pas deja inscrit pour cette annee
   *   - les donnees de l'inscription sont valides
   */
  def inscrire(insc: Inscription): Try[Inscription] = {
    val avecId =
      if (insc.idInscription.isEmpty || insc.idInscription.startsWith("auto"))
        insc.copy(idInscription = inscRepo.prochainId().getOrElse("INS999"))
      else insc

    if (!avecId.estValide)
      return Failure(new IllegalArgumentException(
        "Inscription invalide : matricule, filiere, niveau et annee (format YYYY-YYYY) requis"
      ))

    // Verification etudiant existe
    etuRepo.trouverParMatricule(avecId.matricule).flatMap {
      case None    => Failure(new NoSuchElementException(
        s"Etudiant ${avecId.matricule} introuvable, inscription impossible"
      ))
      case Some(_) =>
        // Verification unicite par annee
        inscRepo.trouverParEtudiantEtAnnee(avecId.matricule, avecId.annee).flatMap {
          case Some(existante) if existante.idInscription != avecId.idInscription =>
            Failure(new IllegalStateException(
              s"L'etudiant ${avecId.matricule} est deja inscrit pour l'annee ${avecId.annee} " +
              s"(inscription ${existante.idInscription})"
            ))
          case _ =>
            inscRepo.enregistrer(avecId)
        }
    }
  }

  /** Modifier le statut d'une inscription (validation, annulation). */
  def changerStatut(id: String, nouveau: StatutInscription): Try[Inscription] =
    inscRepo.trouverParId(id).flatMap {
      case Some(i) => inscRepo.enregistrer(i.copy(statut = nouveau))
      case None    => Failure(new NoSuchElementException(s"Inscription $id introuvable"))
    }

  def supprimer(id: String): Try[Int] = inscRepo.supprimer(id)

  // ---------- Statistiques ----------
  def comptageParStatut(): Try[Map[String, Int]] =
    inscRepo.listerToutes().map { all =>
      all.groupBy(i => StatutInscription.toString(i.statut)).view.mapValues(_.size).toMap
    }

  def comptageParAnnee(): Try[Map[String, Int]] =
    inscRepo.listerToutes().map { all =>
      all.groupBy(_.annee).view.mapValues(_.size).toMap
    }

  def comptageParFiliere(): Try[Map[String, Int]] =
    inscRepo.listerToutes().map { all =>
      all.filter(_.statut == StatutInscription.Validee)
         .groupBy(_.filiere).view.mapValues(_.size).toMap
    }
}
