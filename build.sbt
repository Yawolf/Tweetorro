
lazy val commonSettings = Seq(
  organization := "org.potorrosl",
  version := "0.1.0",
  // set the Scala version used for the project
  scalaVersion := "2.11.7"
)

name:="Tweetorro"

scalaVersion:="2.11.7"

lazy val root = (project in file(".")).
    settings(commonSettings: _*).
  settings(
    // set the name of the project
    name := "Server",
    scalaSource in Compile := baseDirectory.value / "src",

    libraryDependencies ++= Seq(
      "net.debasishg" %% "redisclient" % "3.0"
    ),

    mainClass in (Compile, run) := Some("server.Server")

  )
