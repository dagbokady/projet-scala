package com.bigdata

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object SparkNotes {

  def run(spark: SparkSession): Unit = {

    println("==================================================")
    println("   SPARK - ANALYSE DES NOTES")
    println("==================================================")

    // 1. Charger le fichier CSV
    println("\n-- Chargement du fichier notes.csv --")
    val df = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("data/notes.csv")

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

    // 4. Calculer la moyenne par matiere
    // Formule : 40% controle continu + 60% examen
    println("\n-- Calcul des moyennes par matiere --")
    val dfAvecMoyenne = dfPropre.withColumn(
      "moyenne",
      (col("controle_continu") * 0.4) + (col("examen") * 0.6)
    )
    dfAvecMoyenne.select(
      "matricule",
      "matiere",
      "controle_continu",
      "examen",
      "moyenne"
    ).show()

    // 5. Moyenne generale par etudiant
    println("-- Moyenne generale par etudiant --")
    dfAvecMoyenne.groupBy("matricule")
      .agg(
        avg("moyenne").alias("Moyenne generale"),
        count("matiere").alias("Nombre matieres")
      )
      .orderBy(desc("Moyenne generale"))
      .show()

    // 6. Moyenne par matiere
    println("-- Moyenne par matiere --")
    dfAvecMoyenne.groupBy("matiere")
      .agg(
        avg("moyenne").alias("Moyenne"),
        min("moyenne").alias("Note min"),
        max("moyenne").alias("Note max")
      )
      .orderBy("Moyenne")
      .show()

    // 7. Matiere la plus difficile
    println("-- Matiere la plus difficile --")
    dfAvecMoyenne.groupBy("matiere")
      .agg(avg("moyenne").alias("Moyenne"))
      .orderBy("Moyenne")
      .limit(1)
      .show()

    // 8. Top 5 meilleurs etudiants
    println("-- Top 5 meilleurs etudiants --")
    dfAvecMoyenne.groupBy("matricule")
      .agg(avg("moyenne").alias("Moyenne generale"))
      .orderBy(desc("Moyenne generale"))
      .limit(5)
      .show()

    // 9. Etudiants ajournes (moyenne < 10)
    println("-- Etudiants ajournes --")
    dfAvecMoyenne.groupBy("matricule")
      .agg(avg("moyenne").alias("Moyenne generale"))
      .filter(col("Moyenne generale") < 10)
      .orderBy("Moyenne generale")
      .show()

    // 10. Taux de reussite global
    println("-- Taux de reussite global --")
    val totalEtudiants = dfAvecMoyenne
      .groupBy("matricule")
      .agg(avg("moyenne").alias("Moyenne generale"))
      .count()

    val etudiantsAdmis = dfAvecMoyenne
      .groupBy("matricule")
      .agg(avg("moyenne").alias("Moyenne generale"))
      .filter(col("Moyenne generale") >= 10)
      .count()

    val tauxReussite = (etudiantsAdmis.toDouble / totalEtudiants.toDouble) * 100
    println(f"Taux de reussite global : $tauxReussite%.2f %%")

    // 11. Sauvegarder en CSV
    println("\n-- Sauvegarde des resultats --")
    dfAvecMoyenne
      .coalesce(1)
      .write
      .option("header", "true")
      .mode("overwrite")
      .csv("output/notes_analyses")
    println("Resultats sauvegardes dans output/notes_analyses/")

    println("\n==================================================")
    println("   FIN ANALYSE NOTES")
    println("==================================================")
  }
}