package universite.service

import universite.model._
import universite.repository.{MatiereRepository, NoteRepository}

import scala.annotation.tailrec
import scala.util.Try

/**
 * Service du Module 5 : Gestion des notes.
 *
 * Couvre :
 *   - saisie / mise a jour des notes (CC + examen)
 *   - calcul de la moyenne par matiere    (40% CC + 60% examen)
 *   - calcul de la moyenne ponderee par UE et generale
 *   - detection des notes manquantes / invalides
 *   - decision academique (admis / ajourne / redoublement)
 *   - releve de notes et classement
 *
 * Utilise : recursivite (somme des notes), pattern matching,
 *           fonctions d'ordre superieur, Option et Try.
 */
class NoteService(
  noteRepo:    NoteRepository    = new NoteRepository,
  matiereRepo: MatiereRepository = new MatiereRepository
) {

  // ---------------------------------------------------------------
  // Lecture
  // ---------------------------------------------------------------

  def listerToutes(): Try[List[Note]]                = noteRepo.listerToutes()
  def listerParEtudiant(m: String): Try[List[Note]]  = noteRepo.listerParEtudiant(m)
  def listerParMatiere(m: String): Try[List[Note]]   = noteRepo.listerParMatiere(m)
  def trouverParId(id: String): Try[Option[Note]]    = noteRepo.trouverParId(id)

  // ---------------------------------------------------------------
  // Saisie / modification
  // ---------------------------------------------------------------

  /**
   * Saisit une note. Si l'idNote est vide ou commence par "auto", on en
   * genere un automatiquement. Verifie la validite avant insertion.
   */
  def saisir(note: Note): Try[Note] = {
    val noteAvecId =
      if (note.idNote.isEmpty || note.idNote.startsWith("auto"))
        note.copy(idNote = noteRepo.prochainId().getOrElse("N999"))
      else note

    if (!noteAvecId.estValide)
      Try(throw new IllegalArgumentException(
        s"Note invalide : valeurs hors de l'intervalle [0, 20]"
      ))
    else
      noteRepo.enregistrer(noteAvecId)
  }

  def supprimer(id: String): Try[Int] = noteRepo.supprimer(id)

  // ---------------------------------------------------------------
  // Calculs (programmation fonctionnelle)
  // ---------------------------------------------------------------

  /** Recursion exigee : somme des moyennes (terminale, sur grosses listes). */
  @tailrec
  final def sommeRecursive(notes: List[Note], acc: Double = 0.0): Double = notes match {
    case Nil          => acc
    case head :: tail => sommeRecursive(tail, acc + head.calculer)
  }

  /** Moyenne d'une liste de notes via fonctions d'ordre superieur (map + reduce). */
  def moyenneSimple(notes: List[Note]): Option[Double] = {
    val moyennes = notes.flatMap(_.moyenneOption)
    if (moyennes.isEmpty) None
    else Some(moyennes.sum / moyennes.size)
  }

  /**
   * Moyenne ponderee par les coefficients des matieres.
   * On joint les notes au referentiel matieres pour recuperer les coefs.
   */
  def moyenneGenerale(matricule: String): Try[Option[Double]] =
    for {
      notes    <- noteRepo.listerParEtudiant(matricule)
      matieres <- matiereRepo.listerToutes()
    } yield {
      val mapMat: Map[String, Matiere] = matieres.map(m => m.idMatiere -> m).toMap
      // foldLeft exige par le projet : ponderation
      val (sommePonderee, sommeCoefs) =
        notes.foldLeft((0.0, 0)) { case ((sp, sc), n) =>
          (n.moyenneOption, mapMat.get(n.matiere)) match {
            case (Some(moy), Some(mat)) => (sp + moy * mat.coefficient, sc + mat.coefficient)
            case _                      => (sp, sc)
          }
        }
      if (sommeCoefs == 0) None else Some(sommePonderee / sommeCoefs)
    }

  /** Moyennes par UE pour un etudiant. */
  def moyennesParUe(matricule: String): Try[Map[String, Double]] =
    for {
      notes    <- noteRepo.listerParEtudiant(matricule)
      matieres <- matiereRepo.listerToutes()
    } yield {
      val matMap = matieres.map(m => m.idMatiere -> m).toMap
      notes
        .flatMap(n => matMap.get(n.matiere).flatMap(m => n.moyenneOption.map(mo => (m, mo))))
        .groupBy { case (m, _) => m.ue }
        .view
        .mapValues { groupe =>
          val (sp, sc) = groupe.foldLeft((0.0, 0)) { case ((sp, sc), (m, moy)) =>
            (sp + moy * m.coefficient, sc + m.coefficient)
          }
          if (sc == 0) 0.0 else sp / sc
        }
        .toMap
    }

  /** Moyennes par matiere (toutes notes confondues) pour le tableau de bord. */
  def moyennesParMatiere(): Try[Map[String, Double]] =
    noteRepo.listerToutes().map { all =>
      all.groupBy(_.matiere).map { case (idMat, lst) =>
        val ms = lst.flatMap(_.moyenneOption)
        idMat -> (if (ms.isEmpty) 0.0 else ms.sum / ms.size)
      }
    }

  // ---------------------------------------------------------------
  // Decisions academiques
  // ---------------------------------------------------------------

  /** Decision basee sur la moyenne ponderee d'un etudiant. */
  def decision(matricule: String): Try[Option[DecisionAcademique]] =
    moyenneGenerale(matricule).map(_.map(DecisionAcademique.fromMoyenne))

  // ---------------------------------------------------------------
  // Detection de problemes
  // ---------------------------------------------------------------

  /** Notes manquantes : CC ou examen absent. */
  def notesIncompletes(): Try[List[Note]] =
    noteRepo.listerToutes().map(_.filterNot(_.estComplete))

  /** Notes invalides : valeur hors de [0, 20]. */
  def notesInvalides(): Try[List[Note]] =
    noteRepo.listerToutes().map(_.filterNot(_.estValide))

  /** Liste des etudiants ajournes ou en redoublement. */
  def etudiantsEnEchec(): Try[List[(String, Double, DecisionAcademique)]] =
    noteRepo.listerToutes().map { all =>
      val matricules = all.map(_.matricule).distinct
      matricules.flatMap { mat =>
        // Pattern matching sur l'Option contenue dans le Try
        moyenneGenerale(mat).toOption.flatten match {
          case Some(m) =>
            val d = DecisionAcademique.fromMoyenne(m)
            if (d != DecisionAcademique.Admis) Some((mat, m, d)) else None
          case None => None
        }
      }.sortBy(_._2)
    }

  // ---------------------------------------------------------------
  // Releve de notes et classement
  // ---------------------------------------------------------------

  /** Releve de notes complet : (Note, moyenne, decision implicite). */
  def releveDeNotes(matricule: String): Try[List[(Note, Option[Double])]] =
    noteRepo.listerParEtudiant(matricule).map(_.map(n => (n, n.moyenneOption)))

  /** Classement des etudiants par moyenne generale (decroissant). */
  def classement(): Try[List[(String, Double)]] =
    noteRepo.listerToutes().map { all =>
      all.map(_.matricule).distinct.flatMap { mat =>
        moyenneGenerale(mat).toOption.flatten.map(m => (mat, m))
      }.sortBy(-_._2)
    }
}
