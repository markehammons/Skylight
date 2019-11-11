import java.io.{ByteArrayOutputStream, PrintWriter}
import java.util.spi.ToolProvider


library := "wlroots"

libraryPackage := "wlroots"

xdgShellProtocolLocation := file("/usr/share/wayland-protocols/unstable/xdg-shell/xdg-shell-unstable-v6.xml")

val includeDirectory = file("/usr/include")

headers := Set(
  includeDirectory / "wlr/types/wlr_output.h",
  includeDirectory / "wlr/backend.h",
  includeDirectory / "wlr/render/wlr_renderer.h",
  includeDirectory / "wlr/types/wlr_idle.h",
  includeDirectory / "wlr/types/wlr_gamma_control_v1.h",
  includeDirectory / "wlr/types/wlr_screencopy_v1.h",
  includeDirectory / "wlr/types/wlr_compositor.h",
  includeDirectory / "wlr/types/wlr_primary_selection_v1.h",
  includeDirectory / "wlr/types/wlr_xdg_shell_v6.h",
  includeDirectory / "wlr/types/wlr_surface.h",
  includeDirectory / "wlr/types/wlr_box.h",
  includeDirectory / "wlr/types/wlr_matrix.h"
)

packageMappings := Map(
  file("/usr/include/wlr/backend") -> "wlroots.backend_headers",
  file("/usr/include/bits/types") -> "usr.include.bits.type_headers"
)

clangOptions := Set("-DWLR_USE_UNSTABLE")

includePaths := Set(
  includeDirectory / "wlr",
  includeDirectory / "wayland",
  includeDirectory / "pixman-1",
  includeDirectory / "libxkbcommon"
) + xdgGenDir.value

libraryPaths := Set(
  file("/usr/lib64")
)

outputLibraryName := "wlroots.jar"

lazy val xdgShellProtocolLocation = settingKey[File]("location of xdg-shell-unstable-v6.xml on your system")

lazy val xdgProtocolGen = taskKey[File]("generates the xdg-shell-protocol.h header")

lazy val xdgGenDir = settingKey[File]("where to create the xdg headers") 

xdgGenDir := baseDirectory.value / "include"


xdgProtocolGen := {
  val logger = streams.value
  import scala.sys.process.Process

  val includeDir = xdgGenDir.value

  includeDir.mkdir()

  val proc = Process("wayland-scanner", Seq("server-header", xdgShellProtocolLocation.value.getCanonicalPath, s"${includeDir.getCanonicalPath}/xdg-shell-unstable-v6-protocol.h")).run()
  val exitCode = proc.exitValue()

  if(exitCode != 0) {
    sys.error("failed to generate the xdg protocol")
  }

  xdgGenDir.value
}

jextract := {
  xdgProtocolGen.value
  jextract.value
}