package org.freedesktop.wayland.core

import usr.include.wayland.wayland_server_core_h.wl_listener
import java.foreign.Scope

type WlListener = wl_listener

object WlListener {
  def apply(given s: Scope): wl_listener = {
    s.allocateStruct(classOf[wl_listener])
  }
}