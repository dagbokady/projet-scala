package universite

import universite.model._
import universite.service.InscriptionService

object MainInscriptions extends App {

  println("╔══════════════════════════════════════╗")
  println("║  Module 4 : Gestion des Inscriptions ║")
  println("╚══════════════════════════════════════╝")

  val service = new InscriptionService()
  service.chargerDepuisCsv("data/inscriptions.csv")

  // Afficher toutes
  service.afficherToutes()

  // Résumé avec pattern matching
  service.afficherResumes()

  // En attente
  service.afficherEnAttente()

  // Recherche par id (Option)
  println("\n── Recherche inscription ──")
  service.rechercherParId("INS001") match {
    case Some(i) => println(service.resumeStatut(i))
    case None    => println("Non trouvée")
  }

  // Changer un statut
  println("\n── Validation de INS007 ──")
  service.valider("INS007")
  service.afficherEnAttente()

  // Inscriptions d'un étudiant
  println("\n── Inscriptions de ETU001 ──")
  service.inscriptionsEtudiant("ETU001").foreach(_.afficher())

  // Tentative de double inscription
  println("\n── Test anti-doublon ──")
  try {
    service.inscrire(Inscription("INS999", "ETU001", "Informatique", "M1", "2025-2026", Validee))
  } catch {
    case e: IllegalStateException => println(s"⚠ ${e.getMessage}")
  }

  // Statistiques
  service.statistiques()
}
