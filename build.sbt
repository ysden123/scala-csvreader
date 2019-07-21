lazy val scalaTestVersion = "3.0.8"
lazy val scalaCheckVersion = "1.14.0"
lazy val scalaMockVersion = "4.3.0"
lazy val scalaLoggingVersion = "3.9.2"
lazy val logbackVersion = "1.2.3"
lazy val akkaStreamVersion = "2.5.23"

lazy val commonSettings = Seq(
  organization := "com.stulsoft",
  version := "1.1.0",
  scalaVersion := "2.13.0",
  scalacOptions ++= Seq(
    "-feature",
    "-language:implicitConversions",
    "-language:postfixOps"),
  libraryDependencies ++= Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamVersion % "test",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test",
    "org.scalamock" %% "scalamock" % scalaMockVersion % "test",
    "ch.qos.logback" % "logback-classic" % logbackVersion % "test"

  )
)

lazy val scalaCsvReader = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "scala-csvreader"
  )