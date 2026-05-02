package universite

import universite.service.FormationService

object MainFormations extends App {

  println("╔══════════════════════════════════════╗")
  println("║  Module 3 : Gestion des Formations   ║")
  println("╚══════════════════════════════════════╝")

  val service = new FormationService()
  service.chargerFilieres("data/filieres.csv")
  service.chargerMatieres("data/matieres.csv")

  // Afficher filières et matières
  service.afficherFilieres()
  service.afficherMatieres()

  // Recherche filière (Option)
  println("\n── Recherche filière ──")
  service.rechercherFiliere("Informatique") match {
    case Some(f) => f.afficher()
    case None    => println("Filière introuvable")
  }

  // Matières par enseignant
  println("\n── Matières de ENS005 ──")
  service.matiereParEnseignant("ENS005").foreach(_.afficher())

  // Matières par UE
  println("\n── Matières de l'UE Données ──")
  service.matiereParUE("UE Donnees").foreach(_.afficher())

  // Statistiques et récursivité
  service.statistiques()

  // Volume horaire par enseignant
  println("\n── Volume horaire par enseignant ──")
  service.volumeParEnseignant.foreach { case (ens, vol) =>
    println(s"  $ens : ${vol}h")
  }
}
