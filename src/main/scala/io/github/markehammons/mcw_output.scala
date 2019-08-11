package io.github.markehammons

import java.foreign.memory.{LayoutType, Pointer}
import java.foreign.{NativeTypes, Scope}

import io.github.markehammons.utils.{wl_list_foreach, wl_signal_add}
import usr.include.wayland.wayland_server_core_h.{FI5, wl_listener}
import wlroots.wlr_output_h.wlr_output
import usr.include.time_lib.clock_gettime
import usr.include.bits.type_headers.struct_timespec_h.timespec
import usr.include.bits.time_lib.CLOCK_MONOTONIC
import usr.include.time_lib
import usr.include.wayland.wayland_server_h.wl_resource
import usr.include.wayland.wayland_server_core_lib.wl_display_terminate
import usr.include.wayland.wayland_util_lib.wl_list_remove
import wlroots.backend_lib.wlr_backend_get_renderer
import wlroots.wlr_box_h.wlr_box
import wlroots.wlr_matrix_lib
import wlroots.wlr_matrix_lib.wlr_matrix_project_box
import wlroots.wlr_output_lib.{wlr_output_attach_render, wlr_output_commit}
import wlroots.wlr_renderer_lib.{wlr_render_texture_with_matrix, wlr_renderer_begin, wlr_renderer_clear, wlr_renderer_end}
import wlroots.wlr_surface_lib.{wlr_surface_from_resource, wlr_surface_get_texture, wlr_surface_has_buffer, wlr_surface_send_frame_done}

case class mcw_output(output: wlr_output, server: mcw_server, scope: Scope) {
  val last_frame = scope.allocateStruct(classOf[timespec])
  clock_gettime(CLOCK_MONOTONIC, last_frame.ptr())

  val color = scope.allocateArray(NativeTypes.FLOAT, 4)

  color.set(0,0.4f)
  color.set(1,0.4f)
  color.set(2,0.4f)
  color.set(3,1.0f)

  var dec = 0

  val destroy = scope.allocateStruct(classOf[wl_listener])
  val frame = scope.allocateStruct(classOf[wl_listener])

  frame.notify$set(scope.allocateCallback(output_frame_notify))
  wl_signal_add(output.events$get().frame$ptr(), frame.ptr())


  destroy.notify$set(scope.allocateCallback(output_destroy_notify))
  wl_signal_add(output.events$get().destroy$ptr(), destroy.ptr())

  lazy val output_frame_notify: FI5 = (_: Pointer[wl_listener], data: Pointer[_]) => {

    val wlr_output = data.cast(LayoutType.ofStruct(classOf[wlr_output]))
    val renderer = wlr_backend_get_renderer(
      wlr_output.get().backend$get())


    val timeScope = time_lib.scope().fork()
    val now = timeScope.allocateStruct(classOf[timespec])
    clock_gettime(CLOCK_MONOTONIC, now.ptr())

    wlr_output_attach_render(wlr_output, Pointer.ofNull())
    wlr_renderer_begin(renderer, wlr_output.get().width$get(), wlr_output.get().height$get())

    wlr_renderer_clear(renderer, color.elementPointer())


    wl_list_foreach[wl_resource](server.compositor.get().surface_resources$get) { resource =>
      val scope = wlr_matrix_lib.scope().fork()
      val surface = wlr_surface_from_resource(resource.ptr())


      if(wlr_surface_has_buffer(surface)) {
        val render_box = scope.allocateStruct(classOf[wlr_box])

        render_box.x$set(20)
        render_box.y$set(20)

        render_box.width$set(surface.get().current$get().width$get())
        render_box.height$set(surface.get().current$get().height$get())

        val matrix = scope.allocateArray(NativeTypes.FLOAT, 16)

        wlr_matrix_project_box(matrix.elementPointer(), render_box.ptr(), surface.get().current$get().transform$get(), 0, wlr_output.get().transform_matrix$get().elementPointer())
        wlr_render_texture_with_matrix(renderer, wlr_surface_get_texture(surface), matrix.elementPointer(), 1.0f)
        wlr_surface_send_frame_done(surface, last_frame.ptr())
        scope.close()
      }
    }

    wlr_output_commit(wlr_output)
    wlr_renderer_end(renderer)

    last_frame.tv_sec$set(now.tv_sec$get())
    last_frame.tv_nsec$set(now.tv_nsec$get())
    timeScope.close()
  }

  lazy val output_destroy_notify: FI5 = (_: Pointer[wl_listener], _: Pointer[_]) => {
    wl_list_remove(destroy.link$ptr())
    wl_list_remove(frame.link$ptr())
    wl_display_terminate(server.wl_display)
    scope.close()
    server.removeOutput(this)
  }
}
