package universite.model

import java.time.LocalDate

/**
 * Mode de paiement (sealed pour pattern matching).
 */
sealed trait ModePaiement
object ModePaiement {
  case object MobileMoney extends ModePaiement
  case object Banque       extends ModePaiement
  case object Especes      extends ModePaiement
  case object Cheque       extends ModePaiement
  case object Inconnu      extends ModePaiement

  def fromString(s: String): ModePaiement = s.trim.toLowerCase match {
    case "mobile money" | "mobilemoney" | "mobile" => MobileMoney
    case "banque"                                   => Banque
    case "especes" | "espèces" | "cash"            => Especes
    case "cheque" | "chèque"                        => Cheque
    case _                                          => Inconnu
  }

  def toString(m: ModePaiement): String = m match {
    case MobileMoney => "Mobile Money"
    case Banque      => "Banque"
    case Especes     => "Especes"
    case Cheque      => "Cheque"
    case Inconnu     => "Inconnu"
  }
}

/**
 * Module 8 : Paiement de scolarite.
 *
 * - montantTotal : frais d'inscription dus
 * - montantPaye  : montant deja regle
 * - reste        : calcul du reste a payer (Calculable)
 */
case class Paiement(
  idPaiement:    String,
  matricule:     String,
  montantTotal:  Double,
  montantPaye:   Double,
  datePaiement:  Option[LocalDate],
  mode:          ModePaiement
) extends Identifiable with Affichable with Validable with Calculable {

  override def id: String = idPaiement

  /** Reste a payer : montantTotal - montantPaye, jamais negatif. */
  def reste: Double = math.max(0.0, montantTotal - montantPaye)

  /** Solde : peut etre negatif si trop-percu. */
  def solde: Double = montantTotal - montantPaye

  /** Taux de paiement [0, 1]. */
  def tauxPaiement: Double =
    if (montantTotal <= 0) 0.0 else math.min(1.0, montantPaye / montantTotal)

  def estSolde: Boolean   = reste <= 0.0
  def enRetard: Boolean   = reste > 0.0  // simplifie : dette non resorbee

  override def afficher: String =
    f"[$idPaiement] $matricule - $montantPaye%.0f / $montantTotal%.0f (reste $reste%.0f) - ${ModePaiement.toString(mode)}"

  override def estValide: Boolean =
    matricule.nonEmpty && montantTotal >= 0 && montantPaye >= 0 && montantPaye <= montantTotal

  /** Pour le trait Calculable : calculer renvoie le taux de paiement. */
  override def calculer: Double = tauxPaiement
}
