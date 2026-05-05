package com.bigdata

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object SparkDashboard {

  def run(): Unit = {

    // 1. Creer la session Spark en mode local
    val spark = SparkSession.builder()
      .appName("Gestion Universitaire - Big Data")
      .master("local[*]")
      .config("spark.ui.enabled", "false")
      .config("spark.driver.host", "localhost")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    println("==================================================")
    println("   MODULE 10 - TABLEAU DE BORD BIG DATA")
    println("==================================================")

    // 2. Lancer les analyses
    println("\n>>> ANALYSE DES PAIEMENTS")
    SparkPaiement.run(spark)

    println("\n>>> ANALYSE DES ETUDIANTS")
    SparkEtudiants.run(spark)

    println("\n>>> ANALYSE DES NOTES")
    SparkNotes.run(spark)

    println("\n>>> ANALYSE DES ABSENCES")
    SparkAbsences.run(spark)

    // 3. Synthese finale
    println("\n==================================================")
    println("   SYNTHESE FINALE - TOUS MODULES")
    println("==================================================")

    // Charger tous les CSV
    val dfPaiements  = spark.read.option("header", "true")
      .option("inferSchema", "true").csv("data/paiements.csv")

    val dfEtudiants  = spark.read.option("header", "true")
      .option("inferSchema", "true").csv("data/etudiants.csv")

    val dfNotes      = spark.read.option("header", "true")
      .option("inferSchema", "true").csv("data/notes.csv")

    val dfAbsences   = spark.read.option("header", "true")
      .option("inferSchema", "true").csv("data/absences.csv")

    // Indicateur 1 : Nombre total d'etudiants
    println(s"\nNombre total etudiants : ${dfEtudiants.count()}")

    // Indicateur 2 : Etudiants par filiere
    println("\nEtudiants par filiere :")
    dfEtudiants.groupBy("filiere")
      .agg(count("matricule").alias("Nombre"))
      .orderBy(desc("Nombre"))
      .show()

    // Indicateur 3 : Taux de recouvrement
    val dfAvecReste = dfPaiements.withColumn(
      "reste_a_payer",
      col("montant_total") - col("montant_paye")
    )
    val totalAttendu  = dfAvecReste.agg(sum("montant_total")).first().getDouble(0)
    val totalEncaisse = dfAvecReste.agg(sum("montant_paye")).first().getDouble(0)
    val taux          = (totalEncaisse / totalAttendu) * 100
    println(f"\nTaux de recouvrement : $taux%.2f %%")

    // Indicateur 4 : Matiere la plus difficile
    println("\nMatiere la plus difficile :")
    val dfAvecMoyenne = dfNotes.withColumn(
      "moyenne",
      (col("controle_continu") * 0.4) + (col("examen") * 0.6)
    )
    dfAvecMoyenne.groupBy("matiere")
      .agg(avg("moyenne").alias("Moyenne"))
      .orderBy("Moyenne")
      .limit(1)
      .show()

    // Indicateur 5 : Taux d'absenteisme
    val totalHeures      = dfAbsences.agg(sum("heures")).first().getLong(0)
    val totalHeuresCours = 198
    val tauxAbs          = (totalHeures.toDouble / totalHeuresCours.toDouble) * 100
    println(f"\nTaux d'absenteisme global : $tauxAbs%.2f %%")

    // Indicateur 6 : Top 5 meilleurs etudiants
    println("\nTop 5 meilleurs etudiants :")
    dfAvecMoyenne.groupBy("matricule")
      .agg(avg("moyenne").alias("Moyenne generale"))
      .orderBy(desc("Moyenne generale"))
      .limit(5)
      .show()

    // Indicateur 7 : Etudiants a risque
    println("\nEtudiants a risque (moyenne < 10) :")
    dfAvecMoyenne.groupBy("matricule")
      .agg(avg("moyenne").alias("Moyenne generale"))
      .filter(col("Moyenne generale") < 10)
      .orderBy("Moyenne generale")
      .show()

    // 4. Sauvegarder la synthese
    println("\n-- Sauvegarde synthese finale --")
    dfAvecMoyenne.groupBy("matricule")
      .agg(avg("moyenne").alias("Moyenne generale"))
      .coalesce(1)
      .write
      .option("header", "true")
      .mode("overwrite")
      .csv("output/synthese_finale")
    println("Synthese sauvegardee dans output/synthese_finale/")

    // 5. Fermer Spark
    spark.stop()

    println("\n==================================================")
    println("   FIN DU MODULE 10 - BIG DATA")
    println("==================================================")
  }
}