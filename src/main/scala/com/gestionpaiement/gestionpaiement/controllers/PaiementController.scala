package com.gestionpaiement.controllers

import com.gestionpaiement.models.Paiement
import com.gestionpaiement.services.PaiementService

object PaiementController {

  // Données de test issues du PDF du projet
  val paiements: List[Paiement] = List(
    Paiement("PAY001", "ETU001", 1000000, 700000,  "2025-09-10", "Mobile Money"),
    Paiement("PAY002", "ETU002", 1000000, 1000000, "2025-09-11", "Banque"),
    Paiement("PAY003", "ETU003", 1200000, 600000,  "2025-09-12", "Mobile Money"),
    Paiement("PAY004", "ETU004", 1200000, 900000,  "2025-09-13", "Banque"),
    Paiement("PAY005", "ETU005", 1500000, 1500000, "2025-09-14", "Banque"),
    Paiement("PAY006", "ETU006", 1200000, 500000,  "2025-09-15", "Mobile Money"),
    Paiement("PAY007", "ETU007", 1500000, 300000,  "2025-09-16", "Mobile Money"),
    Paiement("PAY008", "ETU008", 1000000, 800000,  "2025-09-17", "Banque"),
    Paiement("PAY009", "ETU009", 1500000, 1000000, "2025-09-18", "Banque"),
    Paiement("PAY010", "ETU010", 1200000, 1200000, "2025-09-19", "Mobile Money")
  )

  // Filières par matricule (normalement fourni par le Module 1)
  val filiereParMatricule: Map[String, String] = Map(
    "ETU001" -> "Informatique",
    "ETU002" -> "Informatique",
    "ETU003" -> "Data Science",
    "ETU004" -> "Cybersecurite",
    "ETU005" -> "Informatique",
    "ETU006" -> "Data Science",
    "ETU007" -> "Cybersecurite",
    "ETU008" -> "Informatique",
    "ETU009" -> "Data Science",
    "ETU010" -> "Cybersecurite"
  )

  // Lance tous les affichages du module 8
  def run(): Unit = {

    // 1. Synthèse financière complète
    PaiementService.afficherSynthese(paiements)

    // 2. Recherche d'un paiement par matricule (Option)
    println("\n── Recherche paiement ETU003 ──")
    PaiementService.rechercherPaiement(paiements, "ETU003") match {
      case Some(p) => println(s"Trouvé : $p")
      case None    => println("Aucun paiement trouvé.")
    }

    // 3. Synthèse par filière
    println("\n── Montant encaissé par filière ──")
    val synthese = PaiementService.syntheseParFiliere(paiements, filiereParMatricule)
    synthese.foreach { case (filiere, montant) =>
      println(s"  $filiere : $montant CFA")
    }

    // 4. Total récursif
    println("\n── Total paiements (récursif) ──")
    val total = PaiementService.totalPaiementsRecursif(paiements)
    println(s"  Total encaissé : $total CFA")
  }
}