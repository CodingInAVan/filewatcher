ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val filewatcher                = "io.mungos.filewatcher"
lazy val scala3Version              = "3.2.2"
lazy val catsEffectVersion          = "3.4.8"
lazy val scalaTestCatsEffectVersion = "1.4.0"
lazy val slf4jVersion               = "2.0.5"
lazy val log4catsVersion            = "2.5.0"
lazy val circeVersion               = "0.14.0"
lazy val fs2Version                 = "3.6.1"
lazy val http4sVersion              = "0.23.18"
lazy val fs2KafkaVersion            = "2.5.0"
lazy val kafkaVersion               = "3.4.0"

lazy val root = (project in file("."))
  .settings(
    name := "filewatcher",
    scalaVersion := scala3Version,
    organization := filewatcher,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % Test,
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "org.slf4j" % "slf4j-simple" % slf4jVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-fs2" % circeVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.apache.kafka" % "kafka-clients" % kafkaVersion
    )
  )
