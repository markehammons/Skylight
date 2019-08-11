import java.io.{ByteArrayOutputStream, PrintWriter}
import java.util.spi.ToolProvider

import app.millSourcePath
import mill._
import mill.api.Loose
import mill.define.Target
import mill.scalalib._

object javahelp extends JavaModule {
    def library = "wlroots"

    def libraryPackage = "wlroots"

    def headers = Seq(
        os.root / 'usr / 'include / 'wlr / 'types / "wlr_output.h",
        os.root / 'usr / 'include / 'wlr / "backend.h",
        os.root / 'usr / 'include / 'wlr / 'render / "wlr_renderer.h"
    )

    def packageMappings = Map(
        os.root / 'usr / 'include / 'wlr / 'backend -> "wlroots.backend_headers",
        os.root / 'usr / 'include / 'bits / 'types -> "usr.include.bits.type_headers"
    )

    def clangOptions = Set("-DWLR_USE_UNSTABLE")

    def includePaths = Seq(
        os.root / 'usr / 'include / 'wlr,
        //  file("/usr/include/wlr/backend"),
        os.root / 'usr / 'include / 'wayland,
        os.root / 'usr / 'include / "pixman-1",
        //   file("/usr/include/"),
        //  file("/usr/lib64/gcc/x86_64-suse-linux/9/include/")
    )

    def libraryPaths = Seq(
        os.root / 'usr / 'lib64
    )

    def outputLibraryName = "wlroots.jar"


    def unmanagedLib = millSourcePath / 'lib

    override def unmanagedClasspath = T {
        if (!ammonite.ops.exists(millSourcePath / "lib")) Agg()
        else Agg.from(ammonite.ops.ls(millSourcePath / "lib").map(PathRef(_)))
    }

    def runTool(name: String, arguments: String*): Either[String, (String, String)] = {
        val maybeTool: Option[ToolProvider] = {
            val _tool = ToolProvider.findFirst(name)
            if (_tool.isPresent) {
                Some(_tool.get())
            } else {
                None
            }
        }

        val result = for (tool <- maybeTool) yield {
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
          .flatMap { case (code, ret, err) =>
              if (code != 0) {
                  Left(s"failure with code $code: ${ret + err}")
              } else {
                  Right(ret -> err)
              }
          }
    }


    def ipaths = T.sources {
        includePaths.map(PathRef(_))
    }

    //    def lpaths = T.sources{
    //        libraryPaths.map(PathRef(_))
    //    }

    def jextract = T {
        val logger = T.ctx.log

        val outputFile = unmanagedLib / s"${library}.jar"

        val IPaths = ipaths().map(_.path.toIO) /*+ xdgProtocolGen.value*/.flatMap(f => Seq("-I", f.getCanonicalPath))
        val LPaths = libraryPaths.map(_.toIO).flatMap(f => Seq("-L", f.getCanonicalPath))
        val mappings = packageMappings.toSeq.flatMap { case (loc, pack) => Seq("--package-map", s"${loc.toIO.getCanonicalPath}=$pack") }
        val clangOpts = clangOptions.toSeq.flatMap(opt => Seq("-C", opt))
        val headerList = headers.map(_.toIO).map(_.getCanonicalPath)
        val command = headerList ++ mappings ++ IPaths ++ clangOpts ++ LPaths ++
          Seq("--record-library-path", "-l", library, "-t", libraryPackage, "-o", outputFile.toIO.getCanonicalPath)

        logger.info(s"issuing command jextract ${command.mkString(" ")}")

        runTool("jextract", command: _*).fold(logger.error(_), { case (out, err) =>
            logger.error(err)
            logger.info(out)
        })

        outputFile
    }

}

object app extends ScalaModule {
    override def moduleDeps = Seq(javahelp)

    override def unmanagedClasspath: Target[Loose.Agg[PathRef]] = javahelp.unmanagedClasspath
    def scalaVersion = "0.17.0-RC1"

}
