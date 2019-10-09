package usr.include

import java.foreign.{Libraries, NativeTypes}
import java.foreign.annotations.{NativeFunction, NativeHeader}
import java.foreign.memory.Pointer
import java.lang.invoke.MethodHandles


@NativeHeader(
  path = "/usr/include/wayland/wayland-util.h",
  libraries = Array(),
  libraryPaths = Array("/usr/lib64/"),
  resolutionContext = Array(), globals = Array()
)
trait stdlib
  @NativeFunction("(u64:u8u64:u8i32)i32")
  def setenv(name: Pointer[java.lang.Byte], value: Pointer[java.lang.Byte], overwrite: Int): Int

object stdlib {
  val lib = Libraries.bind(MethodHandles.lookup(), classOf[stdlib])
  val scope = Libraries.libraryScope(lib)

  def setenv(name: String, value: String, overwrite: Boolean): Boolean =
    val forked = scope.fork()
    val nameArr = forked.allocateArray(NativeTypes.UINT8, (name + '\0').map(_.toByte).toArray)
    val valueArr = forked.allocateArray(NativeTypes.UINT8, (value + '\0').map(_.toByte).toArray)

    val ret = lib.setenv(nameArr.elementPointer(), valueArr.elementPointer(), if(overwrite) 1 else 0) != 0
    forked.close()
    ret
}