package universite

import universite.service.EnseignantService

object MainEnseignants extends App {

  println("╔══════════════════════════════════════╗")
  println("║  Module 2 : Gestion des Enseignants  ║")
  println("╚══════════════════════════════════════╝")

  val service = new EnseignantService()
  service.chargerDepuisCsv("data/enseignants.csv")

  // Afficher tous
  service.afficherTous()

  // Recherche par id
  println("\n── Recherche ──")
  service.afficherEnseignant("ENS001")
  service.afficherEnseignant("ENS999") // inexistant

  // Recherche récursive
  println("\n── Recherche récursive ENS003 ──")
  service.rechercherRecursif(service.tous, "ENS003") match {
    case Some(e) => e.afficher()
    case None    => println("Non trouvé")
  }

  // Affectation cours
  println("\n── Affectation de cours ──")
  service.affecterCours("ENS001", "MAT001")
  service.affecterCours("ENS001", "MAT003")
  service.affecterCours("ENS002", "MAT002")

  // Cours par enseignant
  println("\n── Cours de ENS001 ──")
  service.afficherCoursEnseignant("ENS001")

  // Par département
  service.afficherParDepartement("Informatique")

  // Statistiques
  service.statistiques()
}
