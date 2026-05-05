package com.bigdata

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object SparkAbsences {

  def run(spark: SparkSession): Unit = {

    println("==================================================")
    println("   SPARK - ANALYSE DES ABSENCES")
    println("==================================================")

    // 1. Charger le fichier CSV
    println("\n-- Chargement du fichier absences.csv --")
    val df = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("data/absences.csv")

    // 2. Schema et apercu
    println("Schema :")
    df.printSchema()
    println("Donnees brutes :")
    df.show()

    // 3. Nettoyage
    println("-- Nettoyage --")
    val dfPropre = df.na.drop()
    println(s"Lignes avant nettoyage : ${df.count()}")
    println(s"Lignes apres nettoyage : ${dfPropre.count()}")

    // 4. Total heures d'absence par etudiant
    println("\n-- Total heures absence par etudiant --")
    dfPropre.groupBy("matricule")
      .agg(sum("heures").alias("Total heures"))
      .orderBy(desc("Total heures"))
      .show()

    // 5. Absences non justifiees
    println("-- Absences non justifiees --")
    dfPropre.filter(col("justifiee") === "Non")
      .groupBy("matricule")
      .agg(sum("heures").alias("Heures non justifiees"))
      .orderBy(desc("Heures non justifiees"))
      .show()

    // 6. Etudiants depassant 10 heures d'absence
    println("-- Etudiants avec plus de 10h d'absence --")
    dfPropre.groupBy("matricule")
      .agg(sum("heures").alias("Total heures"))
      .filter(col("Total heures") > 10)
      .orderBy(desc("Total heures"))
      .show()

    // 7. Absences par matiere
    println("-- Absences par matiere --")
    dfPropre.groupBy("matiere")
      .agg(
        sum("heures").alias("Total heures"),
        count("matricule").alias("Nombre absences")
      )
      .orderBy(desc("Total heures"))
      .show()

    // 8. Tendance des absences par mois
    println("-- Tendance absences par mois --")
    dfPropre
      .withColumn("mois", substring(col("date_absence"), 1, 7))
      .groupBy("mois")
      .agg(
        count("matricule").alias("Nombre absences"),
        sum("heures").alias("Total heures")
      )
      .orderBy("mois")
      .show()

    // 9. Taux d'absenteisme global
    println("-- Taux d'absenteisme global --")
    val totalHeures        = dfPropre.agg(sum("heures")).first().getLong(0)
    val totalHeuresCours   = 198  // total volume horaire du PDF
    val tauxAbsenteisme    = (totalHeures.toDouble / totalHeuresCours.toDouble) * 100
    println(f"Total heures absence  : $totalHeures")
    println(f"Total heures de cours : $totalHeuresCours")
    println(f"Taux d'absenteisme    : $tauxAbsenteisme%.2f %%")

    // 10. Sauvegarder en CSV
    println("\n-- Sauvegarde des resultats --")
    dfPropre
      .coalesce(1)
      .write
      .option("header", "true")
      .mode("overwrite")
      .csv("output/absences_analyses")
    println("Resultats sauvegardes dans output/absences_analyses/")

    println("\n==================================================")
    println("   FIN ANALYSE ABSENCES")
    println("==================================================")
  }
}