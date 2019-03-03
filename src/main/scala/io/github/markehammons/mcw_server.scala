package io.github.markehammons

import java.foreign.Scope
import java.foreign.memory.Callback

import io.github.markehammons.main.bytePointerToString
import usr.include.stdlib
import usr.include.wayland.wayland_server_core.wl_listener
import usr.include.wayland.wayland_server_core_h.{wl_display_add_socket_auto, wl_display_create, wl_display_get_event_loop, wl_display_init_shm}
import wlroots.backend_h.wlr_backend_autocreate
import wlroots.wlr_gamma_control_h.wlr_gamma_control_manager_create
import wlroots.wlr_idle_h.wlr_idle_create
import wlroots.wlr_primary_selection_v1_h.wlr_primary_selection_v1_device_manager_create
import wlroots.wlr_screenshooter_h.wlr_screenshooter_create

case class mcw_server(scope: Scope) {
  val wl_display = wl_display_create()
  require(!wl_display.isNull)

  val wl_event_loop = wl_display_get_event_loop(wl_display)
  require(!wl_event_loop.isNull)

  val backend = wlr_backend_autocreate(wl_display, Callback.ofNull())
  require(!backend.isNull)

  val socket = bytePointerToString(wl_display_add_socket_auto(wl_display))
  require(socket != "")

  println(s"Running compositor on wayland display $socket")

  stdlib.setenv("WAYLAND_DISPLAY", socket, true)

  wl_display_init_shm(wl_display)
  wlr_gamma_control_manager_create(wl_display)
  wlr_screenshooter_create(wl_display)
  wlr_primary_selection_v1_device_manager_create(wl_display)
  wlr_idle_create(wl_display)


  val new_output = scope.allocateStruct(classOf[wl_listener])

  var outputs = Set.empty[mcw_output]
}
