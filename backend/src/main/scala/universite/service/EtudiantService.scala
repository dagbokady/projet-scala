package universite.service

import universite.model._
import scala.util.Try
import scala.io.Source

class EtudiantService(private var etudiants: List[Etudiant] = List.empty) {

  // ── Chargement CSV ────────────────────────

  def chargerDepuisCsv(chemin: String): Unit =
    Try(Source.fromFile(chemin)) match {
      case scala.util.Success(source) =>
        val lignes = source.getLines().toList.tail
        source.close()
        etudiants = lignes.flatMap(Etudiant.fromCsv)
        println(s"✓ ${etudiants.length} étudiants chargés.")
      case scala.util.Failure(err) =>
        println(s"✗ Erreur lecture : ${err.getMessage}")
    }

  // ── CRUD ──────────────────────────────────

  def creer(e: Etudiant): Unit = {
    if (!e.estValide)
      throw new IllegalArgumentException(s"Étudiant invalide : ${e.matricule}")
    if (etudiants.exists(_.matricule == e.matricule))
      throw new IllegalStateException(s"Matricule déjà existant : ${e.matricule}")
    etudiants = e :: etudiants
    println(s"✓ ${e.nomComplet} (${e.matricule}) créé.")
  }

  def modifier(matricule: String, nouveau: Etudiant): Unit =
    rechercherParMatricule(matricule) match {
      case Some(_) =>
        etudiants = etudiants.map(e => if (e.matricule == matricule) nouveau else e)
        println(s"✓ Étudiant $matricule modifié.")
      case None =>
        println(s"✗ Étudiant $matricule introuvable.")
    }

  // ── Recherche ─────────────────────────────

  def rechercherParMatricule(matricule: String): Option[Etudiant] =
    etudiants.find(_.matricule == matricule)

  def afficherEtudiant(matricule: String): Unit =
    rechercherParMatricule(matricule) match {
      case Some(e) => e.afficher()
      case None    => println(s"✗ Aucun étudiant avec le matricule $matricule")
    }

  // ── Affichage ─────────────────────────────

  def afficherTous(): Unit = {
    println(s"\n═══ Liste des étudiants (${etudiants.length}) ═══")
    etudiants.foreach(_.afficher())
  }

  // ── Filtres ───────────────────────────────

  def parFiliere(filiere: String): List[Etudiant] =
    etudiants.filter(_.filiere.equalsIgnoreCase(filiere))

  def parNiveau(niveau: String): List[Etudiant] =
    etudiants.filter(_.niveau.equalsIgnoreCase(niveau))

  def parStatut(statut: StatutEtudiant): List[Etudiant] =
    etudiants.filter(_.statut == statut)

  def etudiantsActifs: List[Etudiant]    = parStatut(Actif)
  def etudiantsSuspendus: List[Etudiant] = parStatut(Suspendu)

  // ── Récursivité ───────────────────────────

  def compterActifsRecursif(liste: List[Etudiant]): Int =
    liste match {
      case Nil                                    => 0
      case tete :: queue if tete.statut == Actif => 1 + compterActifsRecursif(queue)
      case _ :: queue                             => compterActifsRecursif(queue)
    }

  def afficherFiliereRecursif(liste: List[Etudiant], filiere: String): Unit =
    liste match {
      case Nil => ()
      case tete :: queue =>
        if (tete.filiere.equalsIgnoreCase(filiere)) tete.afficher()
        afficherFiliereRecursif(queue, filiere)
    }

  // ── Statistiques ──────────────────────────

  def nbParFiliere: Map[String, Int] =
    etudiants.groupBy(_.filiere).map { case (f, lst) => f -> lst.length }

  def statistiques(): Unit = {
    println("\n═══ Statistiques Étudiants ═══")
    println(s"  Total     : ${etudiants.length}")
    println(s"  Actifs    : ${compterActifsRecursif(etudiants)}")
    println(s"  Suspendus : ${etudiantsSuspendus.length}")
    println(s"  Par filière :")
    nbParFiliere.foreach { case (f, n) => println(s"    - $f : $n") }
  }

  def tous: List[Etudiant] = etudiants
}