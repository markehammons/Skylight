package io.github.markehammons

import java.foreign.layout.Group
import java.foreign.memory.{LayoutType, Pointer, Struct}
import java.foreign.{NativeTypes, Scope}

import usr.include.wayland.wayland_server_core.{FI5, wl_listener, wl_signal}
import usr.include.wayland.wayland_server_core_h.{wl_display_destroy, wl_display_run, wl_display_terminate}
import usr.include.wayland.wayland_util.wl_list
import usr.include.wayland.wayland_util_h.{wl_list_insert, wl_list_length, wl_list_remove}
import wlroots.backend_h.{wlr_backend_get_renderer, wlr_backend_start}
import wlroots.wlr_output.{wlr_output, wlr_output_mode}
import wlroots.wlr_output_h.{wlr_output_make_current, wlr_output_set_mode}
import wlroots.wlr_renderer_h.{wlr_renderer_begin, wlr_renderer_clear, wlr_renderer_end}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import implicits._

object main {
  type Listable[T] = {
    def ptr(): Pointer[T]
    def link$ptr(): Pointer[wl_list]
  }

  def offsetOf[T <: Struct[T]](fieldName: String)(implicit classTag: ClassTag[T]) = {
    val g = LayoutType.ofStruct(classTag.runtimeClass.asInstanceOf[Class[T]]).layout().asInstanceOf[Group]

    val bits = for(l <- g.elements().asScala.takeWhile(_.name().filter(_ == fieldName).isEmpty)) yield {
      l.bitsSize()
    }

    bits.sum / 8
  }


  def extractAnonStruct[T,U](t: T)(implicit anonExtractable: HasExtractableEvents[T, U]) = anonExtractable.extractFrom(t)

  def wl_signal_add(signal: Pointer[wl_signal], listener: Pointer[wl_listener]) = wl_list_insert(signal.get().listener_list$get().prev$get(), listener.get().link$ptr())

  def wl_container_of[T <: Listable[T] with Struct[T]](listItem: wl_list)(implicit classTag: ClassTag[T]): Pointer[T] = {
    val clazz = classTag.runtimeClass.asInstanceOf[Class[T]]
    val offset = offsetOf[T]("link")
    listItem.ptr().cast(NativeTypes.VOID).cast(NativeTypes.INT8).offset(offset).cast(NativeTypes.VOID).cast(LayoutType.ofStruct(clazz))
  }

  def wl_container_of[T <: Listable[T] with Struct[T]](listItemPtr: Pointer[wl_list])(implicit classTag: ClassTag[T]): Pointer[T] = {
    wl_container_of(listItemPtr.get)
  }


  def output_frame_notify(output: mcw_output): FI5 = (_: Pointer[wl_listener], data: Pointer[_]) => {
    import output.{color, dec}

    val wlr_output = data.cast(LayoutType.ofStruct(classOf[wlr_output]))
    val renderer = wlr_backend_get_renderer(
      wlr_output.get().backend$get())

    val now = System.currentTimeMillis()

    // Calculate a color, just for pretty demo purposes
    val ms = now - output.last_frame
    val inc = (dec + 1) % 3
    color.set(inc, color.get(inc) + ms / 2000f)
    color.set(dec, color.get(dec) - ms / 2000f)
    if(color.get(dec) < 0f) {
      color.set(inc, 1.0f)
      color.set(dec, 0.0f)
      dec = inc
    }
    // End pretty color calculation


    wlr_output_make_current(wlr_output, Pointer.ofNull())
    wlr_renderer_begin(renderer, wlr_output.get().width$get(), wlr_output.get().height$get())

    wlr_renderer_clear(renderer, output.color.elementPointer())

    wlr_output_workaround.swap_buffers(wlr_output)
    wlr_renderer_end(renderer)

    output.last_frame = now
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

    scope.close()
  }
}
