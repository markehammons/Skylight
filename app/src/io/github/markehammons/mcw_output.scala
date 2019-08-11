package io.github.markehammons

import java.foreign.{NativeTypes, Scope}

import usr.include.wayland.wayland_server_core_h.wl_listener
import wlroots.wlr_output_h.wlr_output

case class mcw_output(output: wlr_output, server: mcw_server, scope: Scope) {
  var last_frame = System.currentTimeMillis()
  val color = scope.allocateArray(NativeTypes.FLOAT, 4l)

  color.set(0,1.0f)
  color.set(3,1.0f)

  var dec = 0

  val destroy = Scope.globalScope().allocateStruct(classOf[wl_listener])
  val frame = Scope.globalScope().allocateStruct(classOf[wl_listener])

  server.outputs += this

  def free(): Unit = {
    scope.close()
    server.outputs -= this
  }
}
