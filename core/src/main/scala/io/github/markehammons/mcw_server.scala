package io.github.markehammons

import java.foreign.Scope
import java.foreign.memory.{Callback, LayoutType, Pointer}

import io.github.markehammons.utils._
import usr.include.stdlib
import usr.include.wayland.wayland_server_core_h.{FI5, wl_listener, wl_event_loop}
import usr.include.wayland.wayland_server_core_h.wl_display
import usr.include.wayland.wayland_server_core_lib.{wl_display_add_socket_auto, wl_display_create, wl_display_get_event_loop, wl_display_init_shm, wl_display_run, wl_display_destroy}
import usr.include.wayland.wayland_util_lib.wl_list_length
import wlroots.backend_lib.{wlr_backend_autocreate, wlr_backend_get_renderer}
import wlroots.wlr_compositor_lib.wlr_compositor_create
import wlroots.wlr_gamma_control_v1_lib.wlr_gamma_control_manager_v1_create
import wlroots.wlr_idle_lib.wlr_idle_create
import wlroots.wlr_output_h.{wlr_output, wlr_output_mode}
import wlroots.wlr_output_lib.{wlr_output_create_global, wlr_output_set_mode}
import wlroots.wlr_primary_selection_v1_lib.wlr_primary_selection_v1_device_manager_create
import wlroots.wlr_screencopy_v1_lib.wlr_screencopy_manager_v1_create
import wlroots.wlr_xdg_shell_v6_lib.wlr_xdg_shell_v6_create

type WlDisplay = Pointer[wl_display]

object WlDisplay
  def apply(): WlDisplay =
    val display = wl_display_create
    require(!display.isNull)
    display
  



class mcw_server(given Scope)
  given (given scope: Scope): Scope = scope.fork

  val wl_display = WlDisplay()

  val wl_event_loop = wl_display.getEventLoop()

  val backend = wlr_backend_autocreate(wl_display, Callback.ofNull())
  require(!backend.isNull)

  val socket = bytePointerToString(wl_display_add_socket_auto(wl_display))
  require(socket != "")

  println(s"Running compositor on wayland display $socket")

  stdlib.setenv("WAYLAND_DISPLAY", socket, true)

  wl_display_init_shm(wl_display)
  wlr_gamma_control_manager_v1_create(wl_display)
  wlr_screencopy_manager_v1_create(wl_display)
  wlr_primary_selection_v1_device_manager_create(wl_display)
  wlr_idle_create(wl_display)


  wlr_xdg_shell_v6_create(wl_display)

  val compositor = wlr_compositor_create(wl_display, wlr_backend_get_renderer(backend))
  require(!compositor.isNull)

  println("allocating new output")
  val new_output = allocateStruct(classOf[wl_listener])

  println("allocating callback")
  new_output.notify$set(allocateCallback(new_output_notify))
  wl_signal_add(backend.get().events$get().new_output$ptr(), new_output.ptr())

  lazy val new_output_notify: FI5 = (_: Pointer[wl_listener], data: Pointer[_]) => {
    val wlr_output = data.cast(LayoutType.ofStruct(classOf[wlr_output]))

    if(wl_list_length(wlr_output.get().modes$ptr()) > 0)
      val mode =
        wl_container_of[wlr_output_mode](wlr_output.get().modes$get().prev$get().get())
      wlr_output_set_mode(wlr_output, mode)

    outputs += mcw_output(wlr_output.get(), this)

    wlr_output_create_global(wlr_output)
  }

  def removeOutput(output: mcw_output) =
    outputs -= output


  private var outputs = Set.empty[mcw_output]

  trait WlDisplayOps
    def (display: WlDisplay) initShm(): Int =
      wl_display_init_shm(display)

    def (display: WlDisplay) run(): Unit =
      wl_display_run(display)

    def (display: WlDisplay) destroy(): Unit =
      wl_display_destroy(display)

    def (display: WlDisplay) getEventLoop(): Pointer[wl_event_loop] =
      val el = wl_display_get_event_loop(display)
      require(!el.isNull)
      el

  given WlDisplayOps