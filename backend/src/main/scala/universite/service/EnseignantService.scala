package universite.service

import universite.model.Enseignant
import scala.util.Try
import scala.io.Source

// ─────────────────────────────────────────────
//  Module 2 : Service Enseignant
// ─────────────────────────────────────────────

class EnseignantService(private var enseignants: List[Enseignant] = List.empty) {

  // ── Chargement CSV ────────────────────────

  def chargerDepuisCsv(chemin: String): Unit =
    Try(Source.fromFile(chemin)) match {
      case scala.util.Success(source) =>
        val lignes = source.getLines().toList.tail
        source.close()
        enseignants = lignes.flatMap(Enseignant.fromCsv)
        println(s"✓ ${enseignants.length} enseignants chargés.")
      case scala.util.Failure(err) =>
        println(s"✗ Erreur lecture : ${err.getMessage}")
    }

  // ── CRUD ──────────────────────────────────

  def enregistrer(e: Enseignant): Unit = {
    if (!e.estValide)
      throw new IllegalArgumentException(s"Enseignant invalide : ${e.idEnseignant}")
    if (enseignants.exists(_.idEnseignant == e.idEnseignant))
      throw new IllegalStateException(s"Enseignant déjà enregistré : ${e.idEnseignant}")
    enseignants = e :: enseignants
    println(s"✓ ${e.nomComplet} (${e.idEnseignant}) enregistré.")
  }

  // ── Affectation cours ─────────────────────

  def affecterCours(idEnseignant: String, idMatiere: String): Unit =
    rechercherParId(idEnseignant) match {
      case Some(ens) =>
        enseignants = enseignants.map { e =>
          if (e.idEnseignant == idEnseignant) e.affecterCours(idMatiere) else e
        }
        println(s"✓ Cours $idMatiere affecté à ${ens.nomComplet}")
      case None =>
        println(s"✗ Enseignant $idEnseignant introuvable")
    }

  // ── Recherche ─────────────────────────────

  def rechercherParId(id: String): Option[Enseignant] =
    enseignants.find(_.idEnseignant == id)

  def rechercherParNom(nom: String): List[Enseignant] =
    enseignants.filter(_.nom.equalsIgnoreCase(nom))

  def afficherEnseignant(id: String): Unit =
    rechercherParId(id) match {
      case Some(e) => e.afficher()
      case None    => println(s"✗ Enseignant $id introuvable")
    }

  // ── Filtres ───────────────────────────────

  def parDepartement(dept: String): List[Enseignant] =
    enseignants.filter(_.departement.equalsIgnoreCase(dept))

  // ── Récursivité ───────────────────────────

  def rechercherRecursif(liste: List[Enseignant], id: String): Option[Enseignant] =
    liste match {
      case Nil                              => None
      case tete :: _ if tete.idEnseignant == id => Some(tete)
      case _ :: queue                       => rechercherRecursif(queue, id)
    }

  // ── Affichage ─────────────────────────────

  def afficherTous(): Unit = {
    println(s"\n═══ Liste des enseignants (${enseignants.length}) ═══")
    enseignants.foreach(_.afficher())
  }

  def afficherParDepartement(dept: String): Unit = {
    val liste = parDepartement(dept)
    println(s"\n═══ Enseignants – $dept (${liste.length}) ═══")
    liste.foreach(_.afficher())
  }

  def afficherCoursEnseignant(id: String): Unit =
    rechercherParId(id) match {
      case Some(e) =>
        if (e.coursAffecter.isEmpty)
          println(s"Aucun cours affecté à ${e.nomComplet}")
        else
          println(s"Cours de ${e.nomComplet} : ${e.coursAffecter.mkString(", ")}")
      case None =>
        println(s"✗ Enseignant $id introuvable")
    }

  // ── Statistiques ──────────────────────────

  def parDepartementMap: Map[String, List[Enseignant]] =
    enseignants.groupBy(_.departement)

  def statistiques(): Unit = {
    println("\n═══ Statistiques Enseignants ═══")
    println(s"  Total : ${enseignants.length}")
    println(s"  Par département :")
    parDepartementMap.foreach { case (d, lst) => println(s"    - $d : ${lst.length}") }
  }

  def tous: List[Enseignant] = enseignants
}
