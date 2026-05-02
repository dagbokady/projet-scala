package universite.model

// ─────────────────────────────────────────────
//  Module 3 : Modèles de Formation
//  Hiérarchie : Filiere > Niveau > Semestre > UniteEnseignement > Matiere
// ─────────────────────────────────────────────

/** Matière : unité atomique de la formation */
case class Matiere(
  idMatiere    : String,
  nomMatiere   : String,
  ue           : String,
  coefficient  : Int,
  volumeHoraire: Int,
  idEnseignant : String
) extends Identifiable with Affichable with Validable {

  override def id: String = idMatiere

  override def afficher(): Unit =
    println(f"    [MAT] $idMatiere | $nomMatiere%-30s | Coef:$coefficient | ${volumeHoraire}h | Ens:$idEnseignant")

  override def estValide: Boolean =
    idMatiere.nonEmpty && coefficient > 0 && volumeHoraire > 0
}

object Matiere {
  def fromCsv(ligne: String): Option[Matiere] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 6) None
    else scala.util.Try(
      Matiere(cols(0), cols(1), cols(2), cols(3).toInt, cols(4).toInt, cols(5))
    ).toOption
  }
}

/** Unité d'enseignement : regroupe plusieurs matières */
case class UniteEnseignement(
  idUE    : String,
  nomUE   : String,
  matieres: List[Matiere] = List.empty
) extends Identifiable with Affichable {

  override def id: String = idUE

  override def afficher(): Unit = {
    println(s"   [UE] $nomUE")
    matieres.foreach(_.afficher())
  }

  def totalCoeff : Int = matieres.map(_.coefficient).sum
  def totalHeures: Int = matieres.map(_.volumeHoraire).sum
}

/** Semestre : regroupe plusieurs UE */
case class Semestre(
  idSemestre: String,
  nomSemestre: String,
  ues        : List[UniteEnseignement] = List.empty
) extends Identifiable with Affichable {

  override def id: String = idSemestre

  override def afficher(): Unit = {
    println(s"  [SEM] $nomSemestre")
    ues.foreach(_.afficher())
  }
}

/** Niveau (M1, M2…) : regroupe plusieurs semestres */
case class Niveau(
  idNiveau : String,
  nomNiveau: String,
  semestres: List[Semestre] = List.empty
) extends Identifiable with Affichable {

  override def id: String = idNiveau

  override def afficher(): Unit = {
    println(s" [NIV] $nomNiveau")
    semestres.foreach(_.afficher())
  }
}

/** Filière : sommet de la hiérarchie de formation */
case class Filiere(
  idFiliere  : String,
  nomFiliere : String,
  responsable: String,
  niveaux    : List[Niveau] = List.empty
) extends Identifiable with Affichable with Validable {

  override def id: String = idFiliere

  override def afficher(): Unit = {
    println(s"╔═ [FIL] $nomFiliere | Responsable : $responsable")
    niveaux.foreach(_.afficher())
  }

  override def estValide: Boolean = idFiliere.nonEmpty && nomFiliere.nonEmpty
}

object Filiere {
  def fromCsv(ligne: String): Option[Filiere] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 3) None
    else scala.util.Try(Filiere(cols(0), cols(1), cols(2))).toOption
  }
}
