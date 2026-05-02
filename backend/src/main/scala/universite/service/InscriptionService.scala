package universite.service

import universite.model._
import scala.util.Try
import scala.io.Source

// ─────────────────────────────────────────────
//  Module 4 : Service Inscription
// ─────────────────────────────────────────────

class InscriptionService(private var inscriptions: List[Inscription] = List.empty) {

  // ── Chargement CSV ────────────────────────

  def chargerDepuisCsv(chemin: String): Unit =
    Try(Source.fromFile(chemin)) match {
      case scala.util.Success(src) =>
        inscriptions = src.getLines().toList.tail.flatMap(Inscription.fromCsv)
        src.close()
        println(s"✓ ${inscriptions.length} inscriptions chargées.")
      case scala.util.Failure(err) =>
        println(s"✗ Erreur lecture : ${err.getMessage}")
    }

  // ── Inscrire ──────────────────────────────

  /**
   * Inscrit un étudiant.
   * Vérifie qu'il n'est pas déjà inscrit la même année dans la même filière.
   */
  def inscrire(ins: Inscription): Unit = {
    if (!ins.estValide)
      throw new IllegalArgumentException(s"Inscription invalide : ${ins.idInscription}")

    val dejaInscrit = inscriptions.exists(i =>
      i.matricule       == ins.matricule &&
      i.anneeAcademique == ins.anneeAcademique &&
      i.filiere         == ins.filiere
    )

    if (dejaInscrit)
      throw new IllegalStateException(
        s"${ins.matricule} déjà inscrit en ${ins.filiere} (${ins.anneeAcademique})"
      )

    inscriptions = ins :: inscriptions
    println(s"✓ ${ins.matricule} inscrit en ${ins.filiere} ${ins.niveau} — ${ins.anneeAcademique}")
  }

  // ── Changements de statut ─────────────────

  def valider(idInscription: String): Unit  = changerStatut(idInscription, Validee)
  def annuler(idInscription: String): Unit  = changerStatut(idInscription, Annulee)
  def mettreEnAttente(id: String): Unit     = changerStatut(id, EnAttente)

  private def changerStatut(id: String, statut: StatutInscription): Unit =
    rechercherParId(id) match {
      case Some(_) =>
        inscriptions = inscriptions.map(i =>
          if (i.idInscription == id) i.copy(statut = statut) else i
        )
        println(s"✓ Inscription $id → $statut")
      case None =>
        println(s"✗ Inscription $id introuvable")
    }

  // ── Recherche ─────────────────────────────

  def rechercherParId(id: String): Option[Inscription] =
    inscriptions.find(_.idInscription == id)

  def inscriptionsEtudiant(matricule: String): List[Inscription] =
    inscriptions.filter(_.matricule == matricule)

  def inscriptionsParFiliere(filiere: String): List[Inscription] =
    inscriptions.filter(_.filiere.equalsIgnoreCase(filiere))

  def inscriptionsParAnnee(annee: String): List[Inscription] =
    inscriptions.filter(_.anneeAcademique == annee)

  // ── Pattern matching ──────────────────────

  def resumeStatut(ins: Inscription): String =
    ins.statut match {
      case Validee   => s"✅ ${ins.matricule} — inscription validée en ${ins.filiere}"
      case EnAttente => s"⏳ ${ins.matricule} — en attente de validation"
      case Annulee   => s"❌ ${ins.matricule} — inscription annulée"
    }

  // ── Récursivité ───────────────────────────

  def compterValideeRecursif(liste: List[Inscription]): Int =
    liste match {
      case Nil                                        => 0
      case tete :: queue if tete.statut == Validee => 1 + compterValideeRecursif(queue)
      case _ :: queue                                 => compterValideeRecursif(queue)
    }

  // ── Affichage ─────────────────────────────

  def afficherToutes(): Unit = {
    println(s"\n═══ Inscriptions (${inscriptions.length}) ═══")
    inscriptions.foreach(_.afficher())
  }

  def afficherEnAttente(): Unit = {
    val liste = inscriptions.filter(_.statut == EnAttente)
    println(s"\n═══ Inscriptions en attente (${liste.length}) ═══")
    liste.foreach(i => println(s"  → ${resumeStatut(i)}"))
  }

  def afficherResumes(): Unit = {
    println("\n═══ Résumé de toutes les inscriptions ═══")
    inscriptions.foreach(i => println(resumeStatut(i)))
  }

  // ── Statistiques ──────────────────────────

  def nbInscritsParFiliere: Map[String, Int] =
    inscriptions.filter(_.estActive).groupBy(_.filiere).map { case (f, lst) => f -> lst.length }

  def statistiques(): Unit = {
    println("\n═══ Statistiques Inscriptions ═══")
    println(s"  Total      : ${inscriptions.length}")
    println(s"  Validées   : ${compterValideeRecursif(inscriptions)}")
    println(s"  En attente : ${inscriptions.count(_.statut == EnAttente)}")
    println(s"  Annulées   : ${inscriptions.count(_.statut == Annulee)}")
    println(s"  Par filière (validées) :")
    nbInscritsParFiliere.foreach { case (f, n) => println(s"    - $f : $n") }
  }

  def toutes: List[Inscription] = inscriptions
}
