package universite.service

import universite.model._
import universite.repository.{EtudiantRepository, PaiementRepository}

import scala.annotation.tailrec
import scala.util.Try

/**
 * Service du Module 8 : Gestion des paiements.
 *
 * Couvre :
 *  - enregistrement des frais et paiements
 *  - calcul du reste a payer (Calculable)
 *  - detection des etudiants en retard
 *  - synthese financiere par filiere
 */
class PaiementService(
  paieRepo: PaiementRepository = new PaiementRepository,
  etuRepo:  EtudiantRepository = new EtudiantRepository
) {

  // ---------- Lecture ----------
  def listerTous(): Try[List[Paiement]]                       = paieRepo.listerTous()
  def trouverParId(id: String): Try[Option[Paiement]]         = paieRepo.trouverParId(id)
  def listerParEtudiant(matricule: String): Try[List[Paiement]] = paieRepo.listerParEtudiant(matricule)

  // ---------- Recursivite ----------
  /**
   * Recursion exigee : total des montants payes (terminale).
   */
  @tailrec
  final def totalPayeRecursif(paiements: List[Paiement], acc: Double = 0.0): Double =
    paiements match {
      case Nil          => acc
      case head :: tail => totalPayeRecursif(tail, acc + head.montantPaye)
    }

  /** Recursion : total des frais dus. */
  @tailrec
  final def totalDuRecursif(paiements: List[Paiement], acc: Double = 0.0): Double =
    paiements match {
      case Nil          => acc
      case head :: tail => totalDuRecursif(tail, acc + head.montantTotal)
    }

  // ---------- Indicateurs financiers ----------

  /** Montant total encaisse pour un etudiant. */
  def totalPayeEtudiant(matricule: String): Try[Double] =
    paieRepo.listerParEtudiant(matricule).map(totalPayeRecursif(_))

  /** Reste a payer pour un etudiant (somme de tous ses restes). */
  def resteEtudiant(matricule: String): Try[Double] =
    paieRepo.listerParEtudiant(matricule).map { ps =>
      ps.foldLeft(0.0)((acc, p) => acc + p.reste)
    }

  /** Liste des etudiants ayant une dette > 0 (filter + map). */
  def etudiantsEnDette(): Try[List[(String, Double)]] =
    paieRepo.listerTous().map { ps =>
      ps.filter(_.reste > 0)
        .groupBy(_.matricule)
        .view
        .mapValues(_.foldLeft(0.0)((acc, p) => acc + p.reste))
        .toList
        .sortBy { case (_, dette) => -dette }
    }

  /** Total encaisse global. */
  def totalEncaisse(): Try[Double] =
    paieRepo.listerTous().map(totalPayeRecursif(_))

  /** Total attendu global (frais factures). */
  def totalAttendu(): Try[Double] =
    paieRepo.listerTous().map(totalDuRecursif(_))

  /** Taux de recouvrement = total paye / total du. */
  def tauxRecouvrement(): Try[Double] =
    paieRepo.listerTous().map { ps =>
      val du   = totalDuRecursif(ps)
      val paye = totalPayeRecursif(ps)
      if (du <= 0.0) 0.0 else paye / du
    }

  /**
   * Synthese financiere par filiere :
   *   filiere -> (totalAttendu, totalEncaisse, taux)
   */
  def syntheseParFiliere(): Try[Map[String, (Double, Double, Double)]] =
    for {
      etus <- etuRepo.listerTous()
      paie <- paieRepo.listerTous()
    } yield {
      val parFil: Map[String, List[String]] = etus.groupBy(_.filiere).view.mapValues(_.map(_.matricule)).toMap
      parFil.map { case (fil, mats) =>
        val pertinents = paie.filter(p => mats.contains(p.matricule))
        val du   = pertinents.foldLeft(0.0)((a, p) => a + p.montantTotal)
        val paye = pertinents.foldLeft(0.0)((a, p) => a + p.montantPaye)
        val taux = if (du <= 0) 0.0 else paye / du
        fil -> (du, paye, taux)
      }
    }

  /** Comptage par mode de paiement (Banque / Mobile Money...). */
  def repartitionParMode(): Try[Map[String, Double]] =
    paieRepo.listerTous().map { ps =>
      ps.groupBy(p => ModePaiement.toString(p.mode))
        .view
        .mapValues(_.foldLeft(0.0)((a, p) => a + p.montantPaye))
        .toMap
    }

  // ---------- Ecriture ----------
  def enregistrer(p: Paiement): Try[Paiement] = {
    val avecId =
      if (p.idPaiement.isEmpty || p.idPaiement.startsWith("auto"))
        p.copy(idPaiement = paieRepo.prochainId().getOrElse("PAY999"))
      else p
    if (!avecId.estValide)
      Try(throw new IllegalArgumentException(
        "Paiement invalide : montants positifs et montant_paye <= montant_total"
      ))
    else paieRepo.enregistrer(avecId)
  }

  def supprimer(id: String): Try[Int] = paieRepo.supprimer(id)
}
