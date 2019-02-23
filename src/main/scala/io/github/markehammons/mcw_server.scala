package io.github.markehammons

import java.foreign.Scope
import java.foreign.memory.Callback

import usr.include.wayland.wayland_server_core.wl_listener
import usr.include.wayland.wayland_server_core_h.{wl_display_create, wl_display_get_event_loop}
import wlroots.backend_h.wlr_backend_autocreate

case class mcw_server(scope: Scope) {
  val wl_display = wl_display_create()
  require(!wl_display.isNull)

  val wl_event_loop = wl_display_get_event_loop(wl_display)
  require(!wl_event_loop.isNull)

  val backend = wlr_backend_autocreate(wl_display, Callback.ofNull())
  require(!backend.isNull)

  val new_output = scope.allocateStruct(classOf[wl_listener])

  var outputs = Set.empty[mcw_output]
}
