import java.util.spi.ToolProvider
import java.io.{ByteArrayOutputStream, PrintWriter}

import java.lang.{System => JavaSystem}

name := "Wayland McWayface (JVM-edition)"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.21"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.21"

fork := true

library := "wlroots"

libraryPackage := "wlroots"

javaOptions ++= Seq("-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI","-Dgraal.ShowConfiguration=info")

headers := Set(
  file("/usr/include/wlr/types/wlr_output.h"),
  file("/usr/include/wlr/backend.h"),
  file("/usr/include/wlr/render/wlr_renderer.h"),
)

packageMappings := Map(
  file("/usr/include/wlr/backend") -> "wlroots.backend_headers",
  file("/usr/include/bits/types") -> "usr.include.bits.type_headers"
)

clangOptions := Set("\"-DWLR_USE_UNSTABLE\"")

includePaths := Set(
  file("/usr/include/wlr"),
  file("/usr/include/wayland/"),
  file("/usr/include/pixman-1/")
)

libraryPaths := Set(
  file("/usr/lib64/")
)

outputLibraryName := "wlroots2.jar"

//below is coding for jextract to be run by SBT and configuration to be done within sbt

//this allows me to run jextract from within sbt
def runTool(name: String, arguments: String*): Either[String,String] = {
  println(JavaSystem.getProperty("jextract.debug"))
  println(arguments.mkString(" "))

  val maybeTool: Option[ToolProvider] = {
    val _tool = ToolProvider.findFirst(name)
    if(_tool.isPresent) {
      Some(_tool.get())
    } else {
      None
    }
  }

  val result = for(tool <- maybeTool) yield {
    println(s"running ${tool.name()}")
    val stdOut = new ByteArrayOutputStream()
    val errOut = new ByteArrayOutputStream()
    val code = tool.run(new PrintWriter(System.out), new PrintWriter(System.err), arguments: _*)
    (code, new String(stdOut.toByteArray), new String(errOut.toByteArray))
  }

  result
    .toRight(s"Could not find tool $name in your java development environment")
    .flatMap{ case (code,ret,err) =>
      if(ret.contains("Error:") || err.nonEmpty || code != 0) {
        Left(s"failure with code $code: ${ret + err}")
      } else {
        println(s"return value: $ret")
        println(s"error value: $ret")
        Right(ret -> "")
      }
    }
    .map(_._1)
}

lazy val headers = settingKey[Set[File]]("header files to pass to jextract")
lazy val includePaths = settingKey[Set[File]]("paths to include directories that jextract will need")
lazy val libraryPaths = settingKey[Set[File]]("paths to libraries that jextract will link to")
lazy val clangOptions = settingKey[Set[String]]("settings for jextract to pass to clang")
lazy val library = settingKey[String]("library to link to")
lazy val libraryPackage = settingKey[String]("the package the library bindings will be put in")
lazy val outputLibraryName = settingKey[String]("The output jar name")
lazy val packageMappings = settingKey[Map[File, String]]("A mapping of include folders to packages")

lazy val jextract = taskKey[Unit]("generates a java binding for the configured library")

jextract := {
  val logger = streams.value

  logger.log.info(s"generating java bindings for ${library.value} using jextract")

  val outputFile = unmanagedBase.value / s"${outputLibraryName.value}"

  if(outputFile.exists()) {
    logger.log.warn("deleting already generated binding")
    IO.delete(outputFile)
  }

  val IPaths = includePaths.value.toSeq.flatMap(f => Seq("-I", f.getCanonicalPath))
  val LPaths = libraryPaths.value.toSeq.flatMap(f => Seq("-L", f.getCanonicalPath))
  val mappings = packageMappings.value.toSeq.flatMap{ case (loc, pack) => Seq("-m", s"${loc.getCanonicalPath}=$pack")}
  val clangOpts = clangOptions.value.toSeq.flatMap(opt => Seq("-C", opt))
  val headerList = headers.value.toSeq.map(_.getCanonicalPath)
  val command = headerList ++ mappings ++ IPaths ++ clangOpts ++ LPaths ++
    Seq("--record-library-path", "-l", library.value, "-t", libraryPackage.value, "-o", outputFile.getCanonicalPath)

  logger.log.info(s"issuing command jextract ${command.mkString(" ")}")
  runTool("jextract", command: _*).fold(sys.error, o => logger.log.debug(o))
}
