package io.github.markehammons

import java.foreign.Scope
import java.foreign.memory.{Callback, LayoutType, Pointer}

import io.github.markehammons.utils._
import usr.include.stdlib
import usr.include.wayland.wayland_server_core.{FI5, wl_listener}
import usr.include.wayland.wayland_server_core_h.{wl_display_add_socket_auto, wl_display_create, wl_display_get_event_loop, wl_display_init_shm}
import usr.include.wayland.wayland_util_h.wl_list_length
import wlroots.backend_h.{wlr_backend_autocreate, wlr_backend_get_renderer}
import wlroots.wlr_compositor_h.wlr_compositor_create
import wlroots.wlr_gamma_control_h.wlr_gamma_control_manager_create
import wlroots.wlr_idle_h.wlr_idle_create
import wlroots.wlr_output.{wlr_output, wlr_output_mode}
import wlroots.wlr_output_h.{wlr_output_create_global, wlr_output_set_mode}
import wlroots.wlr_primary_selection_v1_h.wlr_primary_selection_v1_device_manager_create
import wlroots.wlr_screenshooter_h.wlr_screenshooter_create
import wlroots.wlr_xdg_shell_v6_h.wlr_xdg_shell_v6_create

import implicits._

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


  wlr_xdg_shell_v6_create(wl_display)

  val compositor = wlr_compositor_create(wl_display, wlr_backend_get_renderer(backend))
  require(!compositor.isNull)

  val new_output = scope.allocateStruct(classOf[wl_listener])

  new_output.notify$set(scope.allocateCallback[FI5](new_output_notify))
  wl_signal_add(extractAnonStruct(backend).new_output$ptr(), new_output.ptr())

  lazy val new_output_notify: FI5 = (_: Pointer[wl_listener], data: Pointer[_]) => {
    val wlr_output = data.cast(LayoutType.ofStruct(classOf[wlr_output]))

    if(wl_list_length(wlr_output.get().modes$ptr()) > 0) {
      val mode =
        wl_container_of[wlr_output_mode](wlr_output.get().modes$get().prev$get().get())
      wlr_output_set_mode(wlr_output, mode)
    }

    outputs += mcw_output(wlr_output.get(), this, scope.fork())

    wlr_output_create_global(wlr_output)
  }

  def removeOutput(output: mcw_output) = {
    outputs -= output
  }


  private var outputs = Set.empty[mcw_output]
}
