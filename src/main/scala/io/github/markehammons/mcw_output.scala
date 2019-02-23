package io.github.markehammons

import java.foreign.{NativeTypes, Scope}

import usr.include.wayland.wayland_server_core.wl_listener
import wlroots.wlr_output.wlr_output

case class mcw_output(output: wlr_output, server: mcw_server, scope: Scope) {
  var last_frame = System.currentTimeMillis()
  val color = scope.allocateArray(NativeTypes.FLOAT, 4)
  var dec = 0

  val destroy = Scope.globalScope().allocateStruct(classOf[wl_listener])
  val frame = Scope.globalScope().allocateStruct(classOf[wl_listener])

  def trashScope(): Unit = {
    scope.close()
  }
}
