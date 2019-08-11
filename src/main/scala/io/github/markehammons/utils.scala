package io.github.markehammons

import java.foreign.NativeTypes
import java.foreign.layout.Group
import java.foreign.memory.{LayoutType, Pointer, Struct}

import usr.include.wayland.wayland_server_core_h.{wl_listener, wl_signal}
import usr.include.wayland.wayland_util_h.wl_list
import usr.include.wayland.wayland_util_lib.wl_list_insert

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.collection.JavaConverters._

object utils {
  type Listable[T] = {
    def ptr(): Pointer[T]
    def link$ptr(): Pointer[wl_list]
  }

  def offsetOf[T <: Struct[T]](fieldName: String)(implicit classTag: ClassTag[T]) = {
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
}
