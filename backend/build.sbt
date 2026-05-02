name         := "gestion-universitaire"
version      := "1.0"
scalaVersion := "2.12.18"

// Spark (utilisé dans le module Big Data par d'autres membres)
val sparkVersion = "3.3.0"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql"  % sparkVersion
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _                        => MergeStrategy.first
}
