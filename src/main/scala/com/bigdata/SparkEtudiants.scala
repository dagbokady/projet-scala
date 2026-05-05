package com.bigdata

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object SparkEtudiants {

  def run(spark: SparkSession): Unit = {

    println("==================================================")
    println("   SPARK - ANALYSE DES ETUDIANTS")
    println("==================================================")

    // 1. Charger le fichier CSV
    println("\n-- Chargement du fichier etudiants.csv --")
    val df = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("data/etudiants.csv")

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

    // 4. Nombre total d'etudiants
    println("\n-- Nombre total d'etudiants --")
    println(s"Total : ${dfPropre.count()}")

    // 5. Nombre d'etudiants par filiere
    println("\n-- Etudiants par filiere --")
    dfPropre.groupBy("filiere")
      .agg(count("matricule").alias("Nombre"))
      .orderBy(desc("Nombre"))
      .show()

    // 6. Nombre d'etudiants par niveau
    println("-- Etudiants par niveau --")
    dfPropre.groupBy("niveau")
      .agg(count("matricule").alias("Nombre"))
      .orderBy("niveau")
      .show()

    // 7. Nombre d'etudiants par statut
    println("-- Etudiants par statut --")
    dfPropre.groupBy("statut")
      .agg(count("matricule").alias("Nombre"))
      .orderBy("statut")
      .show()

    // 8. Etudiants par filiere et niveau
    println("-- Etudiants par filiere et niveau --")
    dfPropre.groupBy("filiere", "niveau")
      .agg(count("matricule").alias("Nombre"))
      .orderBy("filiere", "niveau")
      .show()

    // 9. Etudiants suspendus
    println("-- Etudiants suspendus --")
    dfPropre.filter(col("statut") === "Suspendu")
      .select("matricule", "nom", "prenom", "filiere", "niveau")
      .show()

    // 10. Sauvegarder en CSV
    println("\n-- Sauvegarde des resultats --")
    dfPropre
      .coalesce(1)
      .write
      .option("header", "true")
      .mode("overwrite")
      .csv("output/etudiants_analyses")
    println("Resultats sauvegardes dans output/etudiants_analyses/")

    println("\n==================================================")
    println("   FIN ANALYSE ETUDIANTS")
    println("==================================================")
  }
}