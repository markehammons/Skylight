fork := true
scalacOptions ++= Seq(
  "-Yindent-colons")

name := "Skylight"

version := "0.1"

libraryDependencies += ("com.typesafe.akka" %% "akka-actor-typed" % "2.5.26").withDottyCompat(scalaVersion.value)


javaOptions ++= Seq("-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI","-Dgraal.ShowConfiguration=info")
