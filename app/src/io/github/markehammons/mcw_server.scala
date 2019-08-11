package io.github.markehammons

import java.foreign.Scope
import java.foreign.memory.{Callback, Pointer}

import usr.include.wayland.wayland_server_core_h.{wl_display, wl_listener}
import usr.include.wayland.wayland_server_core_lib.{wl_display_create, wl_display_get_event_loop}
import wlroots.backend_lib.wlr_backend_autocreate

case class mcw_server(scope: Scope) {
  val wl_display: Pointer[wl_display] = {
    wl_display_create()
  }

  val wl_event_loop = wl_display_get_event_loop(wl_display)
  require(!wl_event_loop.isNull)

  val backend = wlr_backend_autocreate(wl_display, Callback.ofNull())
  require(!backend.isNull)

  val new_output = scope.allocateStruct(classOf[wl_listener])

  var outputs = Set.empty[mcw_output]
}
