package universite

import universite.model._
import universite.service.EtudiantService

object MainEtudiants extends App {

  println("╔══════════════════════════════════════╗")
  println("║  Module 1 : Gestion des Étudiants    ║")
  println("╚══════════════════════════════════════╝")

  val service = new EtudiantService()
  service.chargerDepuisCsv("data/etudiants.csv")

  // Afficher tous
  service.afficherTous()

  // Recherche par matricule
  println("\n── Recherche ──")
  service.afficherEtudiant("ETU001")
  service.afficherEtudiant("ETU999") // inexistant

  // Filtres
  println("\n── Étudiants en Informatique ──")
  service.parFiliere("Informatique").foreach(_.afficher())

  println("\n── Étudiants M1 ──")
  service.parNiveau("M1").foreach(_.afficher())

  println("\n── Étudiants suspendus ──")
  service.etudiantsSuspendus.foreach(_.afficher())

  // Statistiques
  service.statistiques()

  // Récursivité démontrée
  println("\n── Parcours récursif filière Data Science ──")
  service.afficherFiliereRecursif(service.tous, "Data Science")
}
