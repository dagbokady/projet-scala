package universite.service

import universite.model._

import scala.util.Try

/**
 * Service du Module 9 : Tableau de bord academique.
 *
 * Produit les indicateurs decisionnels exiges par le PDF :
 *  - nombre total / par filiere / par niveau / par statut
 *  - taux de reussite par filiere
 *  - moyennes par niveau et filiere
 *  - taux d'absenteisme global et par matiere
 *  - top des meilleurs etudiants
 *  - matiere la plus difficile
 *  - filiere avec le meilleur taux de reussite
 *  - synthese financiere
 *  - etudiants a risque (note < 10 ou absences > seuil)
 */
class TableauDeBordService(
  etudiantService:  EtudiantService    = new EtudiantService,
  enseignantService:EnseignantService  = new EnseignantService,
  noteService:      NoteService        = new NoteService,
  absenceService:   AbsenceService     = new AbsenceService,
  paiementService:  PaiementService    = new PaiementService,
  formationService: FormationService   = new FormationService
) {

  // ============================================================
  // Indicateurs etudiants
  // ============================================================

  case class IndicateursEtudiants(
    total:           Int,
    parFiliere:      Map[String, Int],
    parNiveau:       Map[String, Int],
    actifs:          Int,
    suspendus:       Int,
    diplomes:        Int
  )

  def indicateursEtudiants(): Try[IndicateursEtudiants] =
    for {
      total      <- etudiantService.listerTous().map(_.size)
      parFiliere <- etudiantService.comptageParFiliere()
      parNiveau  <- etudiantService.comptageParNiveau()
      actifs     <- etudiantService.compterActifs()
      suspendus  <- etudiantService.compterSuspendus()
      diplomes   <- etudiantService.compterDiplomes()
    } yield IndicateursEtudiants(total, parFiliere, parNiveau, actifs, suspendus, diplomes)

  // ============================================================
  // Indicateurs notes / reussite
  // ============================================================

  case class IndicateursReussite(
    moyenneGlobale:    Option[Double],
    classement:        List[(String, Double)],
    top5:              List[(String, Double)],
    enEchec:           List[(String, Double, DecisionAcademique)],
    moyenneParMatiere: Map[String, Double],
    matiereLaPlusDifficile: Option[(String, Double)]
  )

  def indicateursReussite(): Try[IndicateursReussite] =
    for {
      classement <- noteService.classement()
      enEchec    <- noteService.etudiantsEnEchec()
      parMat     <- noteService.moyennesParMatiere()
    } yield {
      val moyennes = classement.map(_._2)
      val moyG = if (moyennes.isEmpty) None
                 else Some(moyennes.sum / moyennes.size)
      val top5 = classement.take(5)
      val matDifficile = parMat.toList.sortBy(_._2).headOption
      IndicateursReussite(moyG, classement, top5, enEchec, parMat, matDifficile)
    }

  /** Taux de reussite par filiere = (nb etudiants admis / nb etudiants ayant des notes). */
  def tauxReussiteParFiliere(): Try[Map[String, Double]] =
    for {
      etus       <- etudiantService.listerTous()
      classement <- noteService.classement()
    } yield {
      val moyByMat: Map[String, Double] = classement.toMap
      val parFil: Map[String, List[Etudiant]] = etus.groupBy(_.filiere)
      parFil.map { case (fil, lst) =>
        val avecNotes = lst.flatMap(e => moyByMat.get(e.matricule))
        if (avecNotes.isEmpty) fil -> 0.0
        else {
          val nbAdmis = avecNotes.count(_ >= 10.0)
          fil -> (nbAdmis.toDouble / avecNotes.size)
        }
      }
    }

  /** Filiere avec le meilleur taux de reussite. */
  def filiereTopReussite(): Try[Option[(String, Double)]] =
    tauxReussiteParFiliere().map(_.toList.sortBy(-_._2).headOption)

  // ============================================================
  // Indicateurs absences
  // ============================================================

  case class IndicateursAbsences(
    totalHeures:     Int,
    nonJustifiees:   Int,
    enAlerte:        List[(String, Int)],
    tauxParFiliere:  Map[String, Double],
    rapportParMatiere: Map[String, Int]
  )

  def indicateursAbsences(): Try[IndicateursAbsences] =
    for {
      toutes <- absenceService.listerToutes()
      alerte <- absenceService.etudiantsEnAlerte()
      taux   <- absenceService.tauxParFiliere()
      rap    <- absenceService.rapportParMatiere()
    } yield {
      val nonJ = toutes.filterNot(_.justifiee)
      val totalH = nonJ.foldLeft(0)((acc, a) => acc + a.heures)
      IndicateursAbsences(totalH, nonJ.size, alerte, taux, rap)
    }

  // ============================================================
  // Indicateurs financiers
  // ============================================================

  case class IndicateursFinanciers(
    totalAttendu:      Double,
    totalEncaisse:     Double,
    totalRestant:      Double,
    tauxRecouvrement:  Double,
    enDette:           List[(String, Double)],
    parFiliere:        Map[String, (Double, Double, Double)]
  )

  def indicateursFinanciers(): Try[IndicateursFinanciers] =
    for {
      attendu     <- paiementService.totalAttendu()
      encaisse    <- paiementService.totalEncaisse()
      taux        <- paiementService.tauxRecouvrement()
      enDette     <- paiementService.etudiantsEnDette()
      parFiliere  <- paiementService.syntheseParFiliere()
    } yield IndicateursFinanciers(attendu, encaisse, attendu - encaisse, taux, enDette, parFiliere)

  // ============================================================
  // Top enseignants par volume horaire
  // ============================================================

  def topEnseignantsParVolume(n: Int = 5): Try[List[(String, Int)]] =
    enseignantService.topVolumeHoraire(n)

  // ============================================================
  // Etudiants a risque academique :
  // moyenne < 10 OU absences non justifiees >= 10h
  // ============================================================

  case class EtudiantARisque(
    matricule:     String,
    nom:           String,
    prenom:        String,
    filiere:       String,
    moyenne:       Option[Double],
    heuresAbsence: Int,
    motifs:        List[String]
  )

  def etudiantsARisque(seuilAbsence: Int = 10): Try[List[EtudiantARisque]] =
    for {
      etus    <- etudiantService.listerTous()
      classmt <- noteService.classement()
      alerte  <- absenceService.etudiantsEnAlerte(seuilAbsence)
    } yield {
      val moyByMat: Map[String, Double] = classmt.toMap
      val absByMat: Map[String, Int]    = alerte.toMap
      etus.flatMap { e =>
        val moy = moyByMat.get(e.matricule)
        val abs = absByMat.getOrElse(e.matricule, 0)
        val motifs =
          (if (moy.exists(_ < 10.0)) List(s"moyenne ${moy.map("%.2f".format(_)).getOrElse("--")}") else Nil) ++
          (if (abs >= seuilAbsence)  List(s"$abs h d'absence") else Nil)
        if (motifs.isEmpty) None
        else Some(EtudiantARisque(
          e.matricule, e.nom, e.prenom, e.filiere, moy, abs, motifs
        ))
      }
    }

  // ============================================================
  // Synthese globale (pour le dashboard du frontend)
  // ============================================================

  case class SyntheseGlobale(
    nbEtudiants:       Int,
    nbEnseignants:     Int,
    nbFilieres:        Int,
    nbMatieres:        Int,
    moyenneGlobale:    Option[Double],
    tauxAbsenteisme:   Map[String, Double],
    tauxRecouvrement:  Double,
    tauxReussiteGlobal:Double
  )

  def syntheseGlobale(): Try[SyntheseGlobale] =
    for {
      etus    <- etudiantService.listerTous()
      ens     <- enseignantService.listerTous()
      fil     <- formationService.listerFilieres()
      mat     <- formationService.listerMatieres()
      classmt <- noteService.classement()
      tauxAbs <- absenceService.tauxParFiliere()
      tauxRec <- paiementService.tauxRecouvrement()
    } yield {
      val moyennes = classmt.map(_._2)
      val moyG = if (moyennes.isEmpty) None else Some(moyennes.sum / moyennes.size)
      val tauxReussite =
        if (moyennes.isEmpty) 0.0
        else moyennes.count(_ >= 10).toDouble / moyennes.size
      SyntheseGlobale(
        etus.size, ens.size, fil.size, mat.size,
        moyG, tauxAbs, tauxRec, tauxReussite
      )
    }
}
