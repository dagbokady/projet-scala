package universite.model

trait Identifiable {
  def id: String
}

trait Affichable {
  def afficher(): Unit
}

trait Validable {
  def estValide: Boolean
}

trait Calculable {
  def calculer(): Double
}

trait Recherchable[A] {
  def rechercher(critere: String): Option[A]
}
