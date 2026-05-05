package com.gestionpaiement.models


trait Validable {
  def estValide: Boolean
}


case class Paiement(
  idPaiement   : String,
  matricule    : String,
  montantTotal : Double,
  montantPaye  : Double,
  datePaiement : String,
  mode         : String
) extends Validable {

  // Calcule automatiquement le reste à payer
  def resteAPayer: Double = montantTotal - montantPaye

  // Vérifie si le paiement est complet
  def estSolde: Boolean = montantPaye >= montantTotal

  // Vérifie si les montants sont valides (Validable)
  def estValide: Boolean = montantTotal > 0 && montantPaye >= 0

  // Calcule le taux de paiement en pourcentage
  def tauxPaiement: Double = (montantPaye / montantTotal) * 100

  // Affichage lisible
  override def toString: String =
    s"[$idPaiement] $matricule | Total: $montantTotal | Payé: $montantPaye | Reste: $resteAPayer | Mode: $mode"
}