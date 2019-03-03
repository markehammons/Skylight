package io.github.markehammons

import java.foreign.{NativeTypes, Scope}

import usr.include.wayland.wayland_server_core.wl_listener
import wlroots.wlr_output.wlr_output

import usr.include.time_h.clock_gettime
import usr.include.type_headers.struct_timespec.timespec
import usr.include.bits.time_h.CLOCK_MONOTONIC


case class mcw_output(output: wlr_output, server: mcw_server, scope: Scope) {
  val last_frame = scope.allocateStruct(classOf[timespec])
  clock_gettime(CLOCK_MONOTONIC, last_frame.ptr())

  val color = scope.allocateArray(NativeTypes.FLOAT, 4)

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
