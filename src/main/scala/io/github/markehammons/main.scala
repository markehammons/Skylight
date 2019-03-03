package io.github.markehammons

import java.foreign.layout.Group
import java.foreign.memory.{LayoutType, Pointer, Struct}
import java.foreign.{NativeTypes, Scope}
import java.io.{ByteArrayOutputStream, PrintWriter}
import java.util.spi.ToolProvider

import usr.include.wayland.wayland_server_core.{FI5, wl_listener, wl_signal}
import usr.include.wayland.wayland_server.wl_resource
import usr.include.wayland.wayland_server_core_h.wl_resource_instance_of
import usr.include.wayland.wayland_server_core_h.{wl_display_add_socket_auto, wl_display_destroy, wl_display_init_shm, wl_display_run, wl_display_terminate}
import usr.include.wayland.wayland_util.wl_list
import usr.include.wayland.wayland_util_h.{wl_list_insert, wl_list_length, wl_list_remove}
import wlroots.backend_h.{wlr_backend_get_renderer, wlr_backend_start}
import wlroots.wlr_output.{wlr_output, wlr_output_mode}
import wlroots.wlr_output_h.{wlr_output_create_global, wlr_output_make_current, wlr_output_set_mode}
import wlroots.wlr_renderer_h.{wlr_renderer_begin, wlr_renderer_clear, wlr_renderer_end, wlr_render_texture_with_matrix}
import wlroots.wlr_screenshooter_h.wlr_screenshooter_create
import wlroots.wlr_surface_h.{wlr_surface_from_resource, wlr_surface_has_buffer, wlr_surface_get_texture, wlr_surface_send_frame_done}
import wlroots.wlr_idle_h.wlr_idle_create
import wlroots.wlr_primary_selection_v1_h.wlr_primary_selection_v1_device_manager_create
import wlroots.wlr_gamma_control_h.wlr_gamma_control_manager_create
import usr.include.time_h
import usr.include.type_headers.struct_timespec.timespec
import usr.include.bits.time_h.CLOCK_MONOTONIC
import wlroots.wlr_matrix_h
import wlroots.wlr_matrix_h.wlr_matrix_project_box

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import implicits._
import wlroots.wlr_box.wlr_box

import scala.annotation.tailrec

object main {
  type Listable[T] = {
    def ptr(): Pointer[T]
    def link$ptr(): Pointer[wl_list]
  }

  def offsetOf[T <: Struct[T] with Listable[T]](fieldName: String)(implicit classTag: ClassTag[T]) = {
    val g = LayoutType.ofStruct(classTag.runtimeClass.asInstanceOf[Class[T]]).layout().asInstanceOf[Group]


    val bits = for(l <- g.elements().asScala.takeWhile(_.name().filter(_ == fieldName).isEmpty)) yield {
      l.bitsSize()
    }

    -(bits.sum / 8)
  }

  @tailrec
  def bytePointerToString(p: Pointer[java.lang.Byte], length: Int = 0): String = {
    if(p.isNull || p.offset(length).get() == 0) {
      val arr = p.withSize(length).toArray[Array[Byte]](l => Array.ofDim[Byte](l))
      arr.map(_.toChar).mkString
    } else {
      bytePointerToString(p, length + 1)
    }
  }

  def wl_list_foreach[T <: Struct[T] with Listable[T]: ClassTag](start: Pointer[wl_list])(fn: T => Unit): Unit = {
    @tailrec
    def helper(cur: Pointer[wl_list]): Unit = {
      if(cur != start) {
        fn(wl_container_of[T](cur).get())
        helper(cur.get().next$get())
      }
    }
    helper(start.get().next$get())
  }

  def wl_list_foreach[T <: Struct[T] with Listable[T]: ClassTag](start: wl_list)(fn: T => Unit): Unit = wl_list_foreach(start.ptr())(fn)

  def extractAnonStruct[T,U](t: T)(implicit anonExtractable: HasExtractableEvents[T, U]) = anonExtractable.extractFrom(t)

  def wl_signal_add(signal: Pointer[wl_signal], listener: Pointer[wl_listener]) = wl_list_insert(signal.get().listener_list$get().prev$get(), listener.get().link$ptr())

  def wl_container_of[T <: Listable[T] with Struct[T]](listItem: wl_list)(implicit classTag: ClassTag[T]): Pointer[T] = {
    val clazz = classTag.runtimeClass.asInstanceOf[Class[T]]
    val offset = offsetOf[T]("link")
    val ptr = listItem.ptr().cast(NativeTypes.VOID).cast(NativeTypes.UINT8).offset(offset).cast(NativeTypes.VOID).cast(LayoutType.ofStruct(clazz))
    require(ptr.addr() - listItem.ptr().addr() == offset)
    ptr
  }

  def wl_container_of[T <: Listable[T] with Struct[T]](listItemPtr: Pointer[wl_list])(implicit classTag: ClassTag[T]): Pointer[T] = {
    wl_container_of(listItemPtr.get)
  }


  def output_frame_notify(output: mcw_output): FI5 = (_: Pointer[wl_listener], data: Pointer[_]) => {
    import output.{color, dec}

    val wlr_output = data.cast(LayoutType.ofStruct(classOf[wlr_output]))
    val renderer = wlr_backend_get_renderer(
      wlr_output.get().backend$get())


    val timeScope = time_h.scope().fork()
    val now = timeScope.allocateStruct(classOf[timespec])
    time_h.clock_gettime(CLOCK_MONOTONIC, now.ptr())

    // Calculate a color, just for pretty demo purposes
    output.color.set(0,0.4f)
    output.color.set(1,0.4f)
    output.color.set(2,0.4f)
    output.color.set(3,1.0f)
    // End pretty color calculation

    wlr_output_make_current(wlr_output, Pointer.ofNull())
    wlr_renderer_begin(renderer, wlr_output.get().width$get(), wlr_output.get().height$get())

    wlr_renderer_clear(renderer, output.color.elementPointer())


    wl_list_foreach[wl_resource](output.server.compositor.get().surface_resources$get) { resource =>
      val scope = wlr_matrix_h.scope().fork()
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
        wlr_surface_send_frame_done(surface, output.last_frame.ptr())
        scope.close()
      }
    }

    wlr_output_workaround.swap_buffers(wlr_output)
    wlr_renderer_end(renderer)

    output.last_frame.tv_sec$set(now.tv_sec$get())
    output.last_frame.tv_nsec$set(now.tv_nsec$get())
    timeScope.close()
  }

  def output_destroy_notify(output: mcw_output): FI5 = (_: Pointer[wl_listener], _: Pointer[_]) => {
    wl_list_remove(output.destroy.link$ptr())
    wl_list_remove(output.frame.link$ptr())
    wl_display_terminate(output.server.wl_display)
    output.free()
  }

  def new_output_notify(server: mcw_server, scope: Scope): FI5 = (_: Pointer[wl_listener], data: Pointer[_]) => {
    val wlr_output = data.cast(LayoutType.ofStruct(classOf[wlr_output]))

    if(wl_list_length(wlr_output.get().modes$ptr()) > 0) {
      val mode =
        wl_container_of[wlr_output_mode](wlr_output.get().modes$get().prev$get().get())
      wlr_output_set_mode(wlr_output, mode)
    }

    val output = mcw_output(wlr_output.get(), server, scope.fork())

    output.destroy.notify$set(scope.allocateCallback(output_destroy_notify(output)))
    wl_signal_add(extractAnonStruct(wlr_output.get()).destroy$ptr(), output.destroy.ptr())
    output.frame.notify$set(scope.allocateCallback(output_frame_notify(output)))
    wl_signal_add(extractAnonStruct(wlr_output.get()).frame$ptr(), output.frame.ptr())

    wlr_output_create_global(wlr_output)
  }


  def main(args: Array[String]) = {
    val scope = Scope.globalScope()

    val server = mcw_server(scope)
    require(!server.wl_event_loop.isNull)

    server.new_output.notify$set(scope.allocateCallback(new_output_notify(server, scope)))
    wl_signal_add(extractAnonStruct(server.backend).new_output$ptr(), server.new_output.ptr())


    if(!wlr_backend_start(server.backend)) {
      System.err.println("failed to start backend!")
      wl_display_destroy(server.wl_display)
    } else {
      wl_display_run(server.wl_display)
      wl_display_destroy(server.wl_display)
    }
  }
}
