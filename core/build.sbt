fork := true
scalacOptions ++= Seq(
  "-Yindent-colons")

name := "Skylight"

version := "0.1"

javaOptions ++= Seq("-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI","-Dgraal.ShowConfiguration=info")
