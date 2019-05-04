import java.io.{ByteArrayOutputStream, PrintWriter}
import java.util.spi.ToolProvider


name := "SkylightWM"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"


fork := true

library := "wlroots"

libraryPackage := "wlroots"

javaOptions ++= Seq("-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI","-Dgraal.ShowConfiguration=info")

xdgShellProtocolLocation := file("/usr/share/wayland-protocols/unstable/xdg-shell/xdg-shell-unstable-v6.xml")

includeDirectory := file("/usr/include")

libraryDirectory := file("/usr/lib64")

headers := Set(
  includeDirectory.value / "wlr/types/wlr_output.h",
  includeDirectory.value / "wlr/backend.h",
  includeDirectory.value / "wlr/render/wlr_renderer.h",
  includeDirectory.value / "wlr/types/wlr_idle.h",
  includeDirectory.value / "wlr/types/wlr_gamma_control.h",
  includeDirectory.value / "wlr/types/wlr_screenshooter.h",
  includeDirectory.value / "wlr/types/wlr_compositor.h",
  includeDirectory.value / "wlr/types/wlr_primary_selection_v1.h",
  includeDirectory.value / "wlr/types/wlr_xdg_shell_v6.h",
  includeDirectory.value / "wlr/types/wlr_surface.h",
  includeDirectory.value / "wlr/types/wlr_box.h",
  includeDirectory.value / "wlr/types/wlr_matrix.h"
)

packageMappings := Map(
  file("/usr/include/wlr/backend") -> "wlroots.backend_headers",
  file("/usr/include/bits/types") -> "usr.include.bits.type_headers"
)

clangOptions := Set("-DWLR_USE_UNSTABLE")

includePaths := Set(
  includeDirectory.value / "wlr",
  includeDirectory.value / "wayland",
  includeDirectory.value / "pixman-1",
  includeDirectory.value / "libxkbcommon"
)

libraryPaths := Set(
  libraryDirectory.value
)

outputLibraryName := "wlroots.jar"

//below is coding for jextract to be run by SBT and configuration to be done within sbt

//this allows me to run jextract from within sbt
def runTool(name: String, arguments: String*): Either[String,(String,String)] = {
  val maybeTool: Option[ToolProvider] = {
    val _tool = ToolProvider.findFirst(name)
    if(_tool.isPresent) {
      Some(_tool.get())
    } else {
      None
    }
  }

  val result = for(tool <- maybeTool) yield {
    val stdOut = new ByteArrayOutputStream()
    val errOut = new ByteArrayOutputStream()
    val stdWriter = new PrintWriter(stdOut)
    val errWriter = new PrintWriter(errOut)
    val code = tool.run(stdWriter, errWriter, arguments: _*)
    stdWriter.close()
    errWriter.close()
    (code, new String(stdOut.toByteArray), new String(errOut.toByteArray))
  }

  result
    .toRight(s"Could not find tool $name in your java development environment")
    .flatMap{ case (code,ret,err) =>
      if(code != 0) {
        Left(s"failure with code $code: ${ret + err}")
      } else {
        Right(ret -> err)
      }
    }
}

lazy val includeDirectory = settingKey[File]("location of your system's default include directory")
lazy val headers = settingKey[Set[File]]("header files to pass to jextract")
lazy val xdgShellProtocolLocation = settingKey[File]("location of xdg-shell-unstable-v6.xml on your system")
lazy val includePaths = settingKey[Set[File]]("paths to include directories that jextract will need")
lazy val libraryPaths = settingKey[Set[File]]("paths to libraries that jextract will link to")
lazy val clangOptions = settingKey[Set[String]]("settings for jextract to pass to clang")
lazy val library = settingKey[String]("library to link to")
lazy val libraryDirectory = settingKey[File]("location of .so files on your system")
lazy val libraryPackage = settingKey[String]("the package the library bindings will be put in")
lazy val outputLibraryName = settingKey[String]("The output jar name")
lazy val packageMappings = settingKey[Map[File, String]]("A mapping of include folders to packages")

lazy val jextract = taskKey[Unit]("generates a java binding for the configured library")

lazy val xdgProtocolGen = taskKey[File]("generates the xdg-shell-protocol.h header")

xdgProtocolGen := {
  val logger = streams.value
  import scala.sys.process.Process

  val includeDir = baseDirectory.value / "include"

  includeDir.mkdir()

  val proc = Process("wayland-scanner", Seq("server-header", xdgShellProtocolLocation.value.getCanonicalPath, s"${includeDir.getCanonicalPath}/xdg-shell-unstable-v6-protocol.h")).run()
  val exitCode = proc.exitValue()

  if(exitCode != 0) {
    sys.error("failed to generate the xdg protocol")
  }

  includeDir
}



jextract := {
  val logger = streams.value

  val outputFile = unmanagedBase.value / s"${outputLibraryName.value}"

  unmanagedBase.value.mkdir()

  if(outputFile.exists()) {
    logger.log.warn("deleting already generated binding")
    IO.delete(outputFile)
  }

  val IPaths = (includePaths.value + xdgProtocolGen.value).toSeq.flatMap(f => Seq("-I", f.getCanonicalPath))
  val LPaths = libraryPaths.value.toSeq.flatMap(f => Seq("-L", f.getCanonicalPath))
  val mappings = packageMappings.value.toSeq.flatMap{ case (loc, pack) => Seq("--package-map", s"${loc.getCanonicalPath}=$pack")}
  val clangOpts = clangOptions.value.toSeq.flatMap(opt => Seq("-C", opt))
  val headerList = headers.value.toSeq.map(_.getCanonicalPath)
  val command = headerList ++ mappings ++ IPaths ++ clangOpts ++ LPaths ++
    Seq("--record-library-path", "-l", library.value, "-t", libraryPackage.value, "-o", outputFile.getCanonicalPath)

  logger.log.info(s"issuing command jextract ${command.mkString(" ")}")

  runTool("jextract", command: _*).fold(sys.error, { case (out,err) =>
    logger.log.warn(err)
    logger.log.info(out)
  })
}

(Compile / compile) := (Compile / compile).dependsOn(jextract).value
