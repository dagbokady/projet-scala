package universite.model

case class Filiere(
  idFiliere: String,
  nomFiliere: String,
  responsable: String
) extends Identifiable with Affichable {
  override def id: String = idFiliere
  override def afficher: String = s"[$idFiliere] $nomFiliere (resp. $responsable)"
}

case class Matiere(
  idMatiere:    String,
  nomMatiere:   String,
  ue:           String,
  coefficient:  Int,
  volumeHoraire: Int,
  enseignant:   String
) extends Identifiable with Affichable {
  override def id: String = idMatiere
  override def afficher: String =
    s"[$idMatiere] $nomMatiere (UE: $ue, coef=$coefficient, ${volumeHoraire}h, ens. $enseignant)"
}

case class Salle(
  idSalle:    String,
  nomSalle:   String,
  capacite:   Int,
  typeSalle:  String
) extends Identifiable with Affichable {
  override def id: String = idSalle
  override def afficher: String =
    s"[$idSalle] $nomSalle - $typeSalle (capacite: $capacite)"
}
