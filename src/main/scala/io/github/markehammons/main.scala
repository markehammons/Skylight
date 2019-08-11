package io.github.markehammons

import java.foreign.Scope
import java.foreign.memory.Pointer
import usr.include.wayland.wayland_server_core_h.wl_display

import usr.include.wayland.wayland_server_core_lib.{wl_display_destroy, wl_display_run}
import wlroots.backend_lib.wlr_backend_start
import usr.include.wayland.wayland_server_core_lib.{wl_display_add_socket_auto, wl_display_create, wl_display_get_event_loop, wl_display_init_shm}


object main {
  def main(args: Array[String]) = {
    val scope = Scope.globalScope().fork()

    val server = mcw_server(scope)

    if(!wlr_backend_start(server.backend)) {
      System.err.println("failed to start backend!")
      server.wl_display.destroy()
      wl_display_destroy(server.wl_display)
    } else {
      server.wl_display.run()
      server.wl_display.destroy()
    }
  }
}

given DisplayOps {
  def (displayPtr: Pointer[wl_display]) initShm(): Int = {
    wl_display_init_shm(displayPtr)
  }

  def (displayPtr: Pointer[wl_display]) run(): Unit = {
    wl_display_run(displayPtr)
  }

  def (displayPtr: Pointer[wl_display]) destroy(): Unit = {
    wl_display_destroy(displayPtr)
  }
}
