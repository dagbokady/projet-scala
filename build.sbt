name := "gestion-universitaire-scala"

version := "1.0"

scalaVersion := "2.12.18"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.1",
  "org.apache.spark" %% "spark-sql"  % "3.5.1"
)

fork / run := true
javaOptions ++= Seq("-Dfile.encoding=UTF-8")