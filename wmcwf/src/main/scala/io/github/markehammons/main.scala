package io.github.markehammons

import java.foreign.Scope

import usr.include.wayland.wayland_server_core_h.{wl_display_destroy, wl_display_run}
import wlroots.backend_h.wlr_backend_start

object main {
  def main(args: Array[String]) = {
    val scope = Scope.globalScope().fork()

    val server = mcw_server(scope)

    if(!wlr_backend_start(server.backend)) {
      System.err.println("failed to start backend!")
      wl_display_destroy(server.wl_display)
    } else {
      wl_display_run(server.wl_display)
      wl_display_destroy(server.wl_display)
    }
  }
}
