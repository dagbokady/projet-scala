package universite.model

/**
 * Decision academique attribuee a un etudiant en fonction de sa moyenne.
 */
sealed trait DecisionAcademique
object DecisionAcademique {
  case object Admis        extends DecisionAcademique
  case object Ajourne      extends DecisionAcademique
  case object Redoublement extends DecisionAcademique

  def toString(d: DecisionAcademique): String = d match {
    case Admis        => "Admis"
    case Ajourne      => "Ajourne"
    case Redoublement => "Redoublement"
  }

  /**
   * Regle metier :
   *   moyenne >= 10        => Admis
   *   moyenne >= 8         => Ajourne (rattrapage possible)
   *   moyenne <  8         => Redoublement
   */
  def fromMoyenne(moyenne: Double): DecisionAcademique = moyenne match {
    case m if m >= 10.0 => Admis
    case m if m >= 8.0  => Ajourne
    case _              => Redoublement
  }
}

/**
 * Note d'un etudiant pour une matiere donnee.
 *
 * Formule : moyenne = 40% controle continu + 60% examen
 *
 * controleContinu et examen sont des Option pour gerer les notes manquantes,
 * conformement a l'exigence du projet.
 */
case class Note(
  idNote:           String,
  matricule:        String,
  matiere:          String,
  controleContinu:  Option[Double],
  examen:           Option[Double]
) extends Identifiable with Validable with Calculable with Affichable {

  override def id: String = idNote

  /** Une note est valide si elle est presente ET dans [0, 20]. */
  override def estValide: Boolean = {
    def valid(o: Option[Double]): Boolean = o.forall(v => v >= 0.0 && v <= 20.0)
    valid(controleContinu) && valid(examen)
  }

  /** Une note est "complete" si CC et examen sont tous deux renseignes. */
  def estComplete: Boolean = controleContinu.isDefined && examen.isDefined

  /**
   * Calcule la moyenne ponderee.
   * Si l'une des deux notes manque, on retourne 0.0 (l'absence d'evaluation
   * est consideree comme un 0 par defaut, mais on expose aussi `moyenneOption`).
   */
  override def calculer: Double = moyenneOption.getOrElse(0.0)

  /** Version "safe" qui renvoie None si la note n'est pas complete. */
  def moyenneOption: Option[Double] = (controleContinu, examen) match {
    case (Some(cc), Some(ex)) => Some(0.40 * cc + 0.60 * ex)
    case _                    => None
  }

  override def afficher: String = {
    val cc = controleContinu.map(_.toString).getOrElse("-")
    val ex = examen.map(_.toString).getOrElse("-")
    val mo = moyenneOption.map(m => f"$m%.2f").getOrElse("indisponible")
    f"[$idNote] $matricule / $matiere : CC=$cc, EX=$ex => moyenne=$mo"
  }
}
