package universite.model

/**
 * Traits transversaux du domaine.
 * Le projet exige l'utilisation d'au moins un trait, on en fournit plusieurs
 * pour structurer correctement le modele metier.
 */

trait Identifiable {
  /** Identifiant unique de l'entite. */
  def id: String
}

trait Affichable {
  /** Representation lisible pour l'affichage console. */
  def afficher: String
}

trait Validable {
  /** Indique si l'entite est dans un etat valide. */
  def estValide: Boolean
}

trait Calculable {
  /** Effectue le calcul principal de l'entite (moyenne, total, etc.). */
  def calculer: Double
}

trait Recherchable[A] {
  /** Verifie si l'entite correspond au critere donne. */
  def correspondA(critere: A): Boolean
}
