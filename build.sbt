scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-actor-typed" % "2.7.0-M5",
	"com.typesafe.akka" %% "akka-stream" % "2.7.0-M5"
)

lazy val root = (project in file(".")).enablePlugins(JmhPlugin)
