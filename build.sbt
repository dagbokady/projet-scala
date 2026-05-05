name := "gestion-universitaire-scala"

version := "1.0.0"

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  // PostgreSQL JDBC Driver
  "org.postgresql" % "postgresql" % "42.7.1",

  // HikariCP pour pool de connexions
  "com.zaxxer" % "HikariCP" % "5.1.0",

  // Akka HTTP pour exposer une API REST consommable par le front
  "com.typesafe.akka" %% "akka-http"           % "10.5.3",
  "com.typesafe.akka" %% "akka-stream"         % "2.8.5",
  "com.typesafe.akka" %% "akka-actor-typed"    % "2.8.5",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.3",

  // Spray JSON pour la sérialisation
  "io.spray" %% "spray-json" % "1.3.6",

  // Logback pour le logging
  "ch.qos.logback" % "logback-classic" % "1.4.14",

  // Spark Scala pour le module Big Data (optionnel pour ces 3 modules)
  "org.apache.spark" %% "spark-core" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-sql"  % "3.5.0" % "provided",

  // Tests
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-encoding", "UTF-8"
)

// Point d'entrée principal
Compile / mainClass := Some("universite.Main")
