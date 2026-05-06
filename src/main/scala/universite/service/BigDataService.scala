package universite.service

import universite.model._

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import scala.util.{Try, Using}

/**
 * Service du Module 10 : Big Data.
 *
 * Approche pedagogique : on demontre les principes de traitement
 * Big Data (chargement, nettoyage, agregations, export Parquet/CSV)
 * en utilisant des collections Scala fonctionnelles plutot que Spark
 * (Spark reste optionnel dans build.sbt : %% "provided").
 *
 * Le code est ecrit de maniere a ce qu'il soit facilement portable
 * vers Spark : map, filter, reduce, foldLeft, groupBy.
 */
class BigDataService(
  etuService:  EtudiantService     = new EtudiantService,
  noteService: NoteService         = new NoteService,
  absService:  AbsenceService      = new AbsenceService,
  paiService:  PaiementService     = new PaiementService
) {

  private val outputDir = "output"

  /** S'assure que le repertoire de sortie existe. */
  private def ensureOutputDir(): Unit =
    Files.createDirectories(Paths.get(outputDir))

  // ============================================================
  // Export CSV (equivalent DataFrame.write.csv)
  // ============================================================

  /** Exporte un fichier CSV. Les valeurs contenant ',' ou '"' sont entre guillemets. */
  private def ecrireCsv(chemin: String, entetes: List[String], lignes: List[List[String]]): Try[String] =
    Try {
      ensureOutputDir()
      val fichier = new File(chemin)
      Using.resource(new PrintWriter(fichier)) { pw =>
        pw.println(entetes.mkString(","))
        lignes.foreach { l =>
          val esc = l.map { v =>
            if (v.contains(",") || v.contains("\"") || v.contains("\n"))
              "\"" + v.replace("\"", "\"\"") + "\""
            else v
          }
          pw.println(esc.mkString(","))
        }
      }
      fichier.getAbsolutePath
    }

  /**
   * Exporte le rapport academique complet :
   * matricule, nom, prenom, filiere, moyenne, decision, heures_abs, reste_paiement
   */
  def exporterRapportAcademique(): Try[String] =
    for {
      etus       <- etuService.listerTous()
      classement <- noteService.classement()
      alertesAbs <- absService.etudiantsEnAlerte(seuil = 0)  // toutes
      enDette    <- paiService.etudiantsEnDette()
    } yield {
      val moyByMat = classement.toMap
      val absByMat = alertesAbs.toMap
      val detteByMat = enDette.toMap
      val lignes = etus.map { e =>
        val moy  = moyByMat.get(e.matricule)
        val abs  = absByMat.getOrElse(e.matricule, 0)
        val dette = detteByMat.getOrElse(e.matricule, 0.0)
        val dec  = moy.map(DecisionAcademique.fromMoyenne).map(DecisionAcademique.toString).getOrElse("--")
        List(
          e.matricule, e.nom, e.prenom, e.filiere, e.niveau,
          moy.map("%.2f".format(_)).getOrElse(""),
          dec, abs.toString,
          "%.0f".format(dette)
        )
      }
      val chemin = s"$outputDir/rapport_academique.csv"
      val entetes = List("matricule","nom","prenom","filiere","niveau","moyenne","decision","heures_absence","reste_a_payer")
      ecrireCsv(chemin, entetes, lignes).getOrElse(chemin)
    }

  /** Export performances par matiere. */
  def exporterPerformancesParMatiere(): Try[String] =
    noteService.moyennesParMatiere().map { stats =>
      val lignes = stats.toList.sortBy(-_._2).map { case (idMat, moy) =>
        List(idMat, "%.2f".format(moy))
      }
      val chemin = s"$outputDir/performances_par_matiere.csv"
      ecrireCsv(chemin, List("id_matiere", "moyenne"), lignes).getOrElse(chemin)
    }

  /** Export des indicateurs financiers consolides. */
  def exporterIndicateursFinanciers(): Try[String] =
    paiService.syntheseParFiliere().map { syn =>
      val lignes = syn.toList.map { case (fil, (du, paye, taux)) =>
        List(fil, "%.0f".format(du), "%.0f".format(paye),
             "%.0f".format(du - paye), "%.4f".format(taux))
      }
      val chemin = s"$outputDir/indicateurs_financiers.csv"
      ecrireCsv(chemin,
        List("filiere","montant_attendu","montant_encaisse","montant_restant","taux_recouvrement"),
        lignes
      ).getOrElse(chemin)
    }

  // ============================================================
  // Analyses Big Data (collections fonctionnelles)
  // ============================================================

  /** Detection de valeurs manquantes dans la table notes. */
  def detecterValeursManquantes(): Try[Map[String, Int]] =
    noteService.listerToutes().map { notes =>
      val sansCC    = notes.count(_.controleContinu.isEmpty)
      val sansExam  = notes.count(_.examen.isEmpty)
      val invalides = notes.count(!_.estValide)
      Map(
        "notes_sans_cc"      -> sansCC,
        "notes_sans_examen"  -> sansExam,
        "notes_invalides"    -> invalides
      )
    }

  /**
   * Performance par promotion (annee) : moyenne generale de chaque promotion.
   * Demonstration de groupBy + foldLeft.
   */
  def performanceParPromotion(): Try[Map[String, Double]] =
    for {
      etus    <- etuService.listerTous()
      classmt <- noteService.classement()
    } yield {
      val moyByMat = classmt.toMap
      etus.groupBy(_.annee).view.mapValues { lst =>
        val avecMoy = lst.flatMap(e => moyByMat.get(e.matricule))
        if (avecMoy.isEmpty) 0.0
        else avecMoy.foldLeft(0.0)(_ + _) / avecMoy.size
      }.toMap
    }

  /**
   * Tendance des absences par mois (2025-09, 2025-10...).
   * Demonstration : map + groupBy sur format de date.
   */
  def tendanceAbsencesParMois(): Try[Map[String, Int]] =
    absService.listerToutes().map { all =>
      all.groupBy { a =>
        val d = a.dateAbsence.toString // ISO yyyy-MM-dd
        if (d.length >= 7) d.substring(0, 7) else d
      }.view.mapValues(_.foldLeft(0)((acc, a) => acc + a.heures)).toMap
    }

  /**
   * Tendance des paiements par mois.
   */
  def tendancePaiementsParMois(): Try[Map[String, Double]] =
    paiService.listerTous().map { all =>
      all.groupBy { p =>
        p.datePaiement.map(_.toString).map(d => if (d.length >= 7) d.substring(0, 7) else d).getOrElse("inconnu")
      }.view.mapValues(_.foldLeft(0.0)((acc, p) => acc + p.montantPaye)).toMap
    }

  // ============================================================
  // Lancement de tous les exports
  // ============================================================

  case class RapportExport(
    rapportAcademique:    String,
    performances:         String,
    indicateursFinanciers:String
  )

  /** Genere tous les fichiers de sortie en une seule operation. */
  def exporterTout(): Try[RapportExport] =
    for {
      f1 <- exporterRapportAcademique()
      f2 <- exporterPerformancesParMatiere()
      f3 <- exporterIndicateursFinanciers()
    } yield RapportExport(f1, f2, f3)
}
