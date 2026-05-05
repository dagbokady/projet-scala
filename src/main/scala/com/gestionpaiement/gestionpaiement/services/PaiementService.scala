package com.gestionpaiement.services

import com.gestionpaiement.models.Paiement

object PaiementService {

 
  def resteAPayer(paiement: Paiement): Double =
    paiement.resteAPayer


  def etudiantsEnDette(paiements: List[Paiement]): List[Paiement] =
    paiements.filter(p => p.resteAPayer > 0)


  def montantTotalEncaisse(paiements: List[Paiement]): Double =
    paiements.map(_.montantPaye).sum


  def montantTotalAttendu(paiements: List[Paiement]): Double =
    paiements.map(_.montantTotal).sum


  def montantTotalRestant(paiements: List[Paiement]): Double =
    paiements.map(_.resteAPayer).sum


  def tauxRecouvrement(paiements: List[Paiement]): Double = {
    val attendu = montantTotalAttendu(paiements)
    if (attendu == 0) 0.0
    else (montantTotalEncaisse(paiements) / attendu) * 100
  }


  def syntheseParFiliere(
    paiements: List[Paiement],
    filiereParMatricule: Map[String, String]
  ): Map[String, Double] =
    paiements
      .groupBy(p => filiereParMatricule.getOrElse(p.matricule, "Inconnue"))
      .map { case (filiere, ps) => filiere -> montantTotalEncaisse(ps) }


  def rechercherPaiement(
    paiements: List[Paiement],
    matricule: String
  ): Option[Paiement] =
    paiements.find(_.matricule == matricule)


  def totalPaiementsRecursif(paiements: List[Paiement]): Double =
    paiements match {
      case Nil          => 0.0
      case head :: tail => head.montantPaye + totalPaiementsRecursif(tail)
    }


  def afficherSynthese(paiements: List[Paiement]): Unit = {
    println("═" * 50)
    println("      SYNTHÈSE FINANCIÈRE - MODULE 8")
    println("═" * 50)
    println(s"Total attendu      : ${montantTotalAttendu(paiements)} CFA")
    println(s"Total encaissé     : ${montantTotalEncaisse(paiements)} CFA")
    println(s"Total restant      : ${montantTotalRestant(paiements)} CFA")
    println(f"Taux recouvrement  : ${tauxRecouvrement(paiements)}%.2f %%")
    println("─" * 50)
    println(s"Étudiants en dette : ${etudiantsEnDette(paiements).length}")
    etudiantsEnDette(paiements).foreach(p =>
      println(s"  → ${p.matricule} doit encore ${p.resteAPayer} CFA")
    )
    println("═" * 50)
  }
}