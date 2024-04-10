name := """play-slick-pg-fulltext-example"""
organization := "com.megafarad"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.13"

libraryDependencies ++= Seq(
  "net.codingwell" %% "scala-guice" % "5.1.1",
  "com.github.tminglei" %% "slick-pg" % "0.22.1",
  "org.playframework" %% "play-slick" % "6.1.0",
  "org.playframework" %% "play-slick-evolutions" % "6.1.0",
  "org.postgresql" % "postgresql" % "42.7.2",
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.megafarad.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.megafarad.binders._"
