lazy val scalaTestVersion = "3.2.0-SNAP10"
lazy val scalaCheckVersion = "1.14.0"
lazy val scalaMockVersion = "4.1.0"

lazy val commonSettings = Seq(
  organization := "com.stulsoft",
  version := "0.1.0",
  scalaVersion := "2.12.7",
  scalacOptions ++= Seq(
    "-feature",
    "-language:implicitConversions",
    "-language:postfixOps"),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test",
    "org.scalamock" %% "scalamock" % scalaMockVersion % "test"
  )
)

lazy val scalaCsvReader = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "scala-csvreader"
  )