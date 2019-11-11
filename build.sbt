val commonSettings = Seq(
  scalaVersion := "0.20.0-RC1",
  libraryDependencies ++= Seq(
    ("org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0").withDottyCompat(scalaVersion.value)
  ),
)

codeCommand := Seq("flatpak", "run", "com.visualstudio.code.oss","-n")

libraryDependencies += ("org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0").withDottyCompat(scalaVersion.value)


//libraryDependencies += ("org.scalatest" %% "scalatest" % "3.0.5" % "test").withDottyCompat(scalaVersion.value)


lazy val macros = (project).dependsOn(jextract)
  .settings(commonSettings)

lazy val core = (project)
  .dependsOn(macros,jextract)
  .settings(commonSettings)

lazy val jextract = (project).settings(commonSettings).enablePlugins(Shackle)


//(Compile / compile) := (Compile / compile).dependsOn(jextract).value
