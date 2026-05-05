package com.bigdata

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object SparkPaiement {

  def run(spark: SparkSession): Unit = {

    println("==================================================")
    println("   SPARK - ANALYSE DES PAIEMENTS")
    println("==================================================")

    // 1. Charger le fichier CSV
    println("\n-- Chargement du fichier paiements.csv --")
    val df = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("data/paiements.csv")

    // 2. Afficher le schema et les donnees
    println("Schema :")
    df.printSchema()
    println("Donnees brutes :")
    df.show()

    // 3. Nettoyer les donnees manquantes
    println("-- Nettoyage --")
    val dfPropre = df.na.drop()
    println(s"Lignes avant nettoyage : ${df.count()}")
    println(s"Lignes apres nettoyage : ${dfPropre.count()}")

    // 4. Ajouter colonne reste_a_payer
    println("\n-- Calcul reste a payer --")
    val dfAvecReste = dfPropre.withColumn(
      "reste_a_payer",
      col("montant_total") - col("montant_paye")
    )
    dfAvecReste.select(
      "matricule",
      "montant_total",
      "montant_paye",
      "reste_a_payer",
      "date_paiement",
      "mode"
    ).show()

    // 5. Indicateurs financiers globaux
    println("-- Indicateurs financiers globaux --")
    dfAvecReste.agg(
      sum("montant_total").alias("Total attendu"),
      sum("montant_paye").alias("Total encaisse"),
      sum("reste_a_payer").alias("Total restant"),
      count("matricule").alias("Nombre etudiants")
    ).show()

    // 6. Analyse par mode de paiement
    println("-- Analyse par mode de paiement --")
    dfAvecReste.groupBy("mode")
      .agg(
        count("matricule").alias("Nombre"),
        sum("montant_paye").alias("Montant encaisse"),
        sum("reste_a_payer").alias("Reste a payer")
      )
      .orderBy(desc("Montant encaisse"))
      .show()

    // 7. Analyse par periode (mois)
    println("-- Analyse par periode --")
    dfAvecReste
      .withColumn("mois", substring(col("date_paiement"), 1, 7))
      .groupBy("mois")
      .agg(
        count("matricule").alias("Nombre paiements"),
        sum("montant_paye").alias("Montant encaisse")
      )
      .orderBy("mois")
      .show()

    // 8. Etudiants en dette
    println("-- Etudiants en dette --")
    dfAvecReste.filter(col("reste_a_payer") > 0)
      .select("matricule", "montant_total", "montant_paye", "reste_a_payer")
      .orderBy(desc("reste_a_payer"))
      .show()

    // 9. Taux de recouvrement
    println("-- Taux de recouvrement --")
    val totalAttendu  = dfAvecReste.agg(sum("montant_total")).first().getDouble(0)
    val totalEncaisse = dfAvecReste.agg(sum("montant_paye")).first().getDouble(0)
    val taux          = (totalEncaisse / totalAttendu) * 100
    println(f"Taux de recouvrement global : $taux%.2f %%")

    // 10. Sauvegarder en CSV
    println("\n-- Sauvegarde des resultats --")
    dfAvecReste
      .coalesce(1)
      .write
      .option("header", "true")
      .mode("overwrite")
      .csv("output/paiements_analyses")
    println("Resultats sauvegardes dans output/paiements_analyses/")

    println("\n==================================================")
    println("   FIN ANALYSE PAIEMENTS")
    println("==================================================")
  }
}