package io.github.markehammons

import java.foreign.NativeTypes
import java.foreign.layout.Group
import java.foreign.memory.{LayoutType, Pointer, Struct}

import usr.include.wayland.wayland_server_core_h.{wl_listener, wl_signal}
import usr.include.wayland.wayland_util_h.wl_list
import usr.include.wayland.wayland_util_lib.wl_list_insert

import scala.annotation.tailrec
import scala.reflect.ClassTag

import scala.quoted._
import usr.include.wayland.wayland_server_h.wl_resource

import scala.collection.JavaConverters._

import scala.reflect.Selectable.given

object utils
  type Pointable[T] = {
    val ptr: Pointer[T]
  }


  type Listable[T] = {
    def ptr(): Pointer[T]
    def link$ptr(): Pointer[wl_list]
  }

  // val r: Pointable[wl_list] = ???

  // r.ptr

  // trait CanTreatLikeList[T <: Struct[T]](offset: Long)
  //   def foreach(fn: T => Unit, start: Pointer[wl_list]): Unit =
  //     @tailrec
  //     def helper(cur: Pointer[wl_list]): Unit =
  //       if(cur != start)

  trait Contained[T <: Struct[T],U <: Struct[U]](field: String)(given ct: ClassTag[U])
    val clazz = ct.runtimeClass.asInstanceOf[Class[U]]
    val offset = offsetOf[U](field,clazz)
    def getContainer(r: T): Pointer[U] =
      r.ptr.cast(NativeTypes.VOID).cast(NativeTypes.UINT8).offset(offset).cast(NativeTypes.VOID).cast(LayoutType.ofStruct(clazz))

    def getContainerPtr(r: Pointer[T]): Pointer[U] =
      r.cast(NativeTypes.VOID).cast(NativeTypes.UINT8).offset(offset).cast(NativeTypes.VOID).cast(LayoutType.ofStruct(clazz))

  given Contained[wl_list, wl_resource] = new Contained[wl_list, wl_resource]("link") {}

  // given WlListOps: (wlList: wl_list) {
  //   def foreach[T <: Struct[T]](fn: T => Unit)(given cont: Contained[wl_list,T]): Unit =

  // }

  def (list: Pointer[wl_list]) foreach[T <: Struct[T]](fn: T => Unit)(given cont: Contained[wl_list,T]): Unit = 
    @tailrec
    def helper(cur: Pointer[wl_list]): Unit =
      if(cur != list)
        fn(cont.getContainerPtr(cur).get())
        helper(cur.get().next$get())
    helper(list.get().next$get())


  def (list: wl_list) foreach[T <: Struct[T]](fn: T => Unit)(given cont: Contained[wl_list,T]): Unit = 
    list.ptr.foreach(fn)

  new Contained[wl_list, wl_resource]("la") {}

  //summon[Contained[wl_list, wl_resource]]
  
  def offsetOf[T <: Struct[T]](fieldName: String, clazz: Class[T]): Long =
    import scala.language.implicitConversions
    val g = LayoutType.ofStruct(clazz).layout().asInstanceOf[Group]

    val bits = for(l <- g.elements().asScala.takeWhile(_.name().filter(_ == fieldName).isEmpty)) yield
      l.bitsSize()

    -(bits.sum / 8)

  def offsetOf[T <: Struct[T]](fieldName: String)(implicit classTag: ClassTag[T]): Long =
    offsetOf(fieldName,classTag.runtimeClass.asInstanceOf[Class[T]])

  @tailrec
  def bytePointerToString(p: Pointer[java.lang.Byte], length: Long = 0): String =
    if(p.isNull || p.offset(length).get() == 0)
      val arr = p.withSize(length).toArray[Array[Byte]](l => Array.ofDim[Byte](l))
      arr.map(_.toChar).mkString
    else
      bytePointerToString(p, length + 1)

  def wl_list_foreach[T <: Struct[T] with Listable[T]: ClassTag](start: Pointer[wl_list])(fn: T => Unit): Unit =
    @tailrec
    def helper(cur: Pointer[wl_list]): Unit =
      if(cur != start)
        fn(wl_container_of[T](cur).get())
        helper(cur.get().next$get())
    helper(start.get().next$get())

  def wl_list_foreach[T <: Struct[T] with Listable[T]: ClassTag](start: wl_list)(fn: T => Unit): Unit = wl_list_foreach(start.ptr())(fn)
  
  inline def wl_signal_add(signal: Pointer[wl_signal], listener: Pointer[wl_listener]) = wl_list_insert(signal.get().listener_list$get().prev$get(), listener.get().link$ptr())


  def wl_container_of[T <: Listable[T] with Struct[T]](listItem: wl_list)(implicit classTag: ClassTag[T]): Pointer[T] = 
    val clazz = classTag.runtimeClass.asInstanceOf[Class[T]]
    val offset = offsetOf[T]("link")
    val ptr = listItem.ptr().cast(NativeTypes.VOID).cast(NativeTypes.UINT8).offset(offset).cast(NativeTypes.VOID).cast(LayoutType.ofStruct(clazz))
    require(ptr.addr() - listItem.ptr().addr() == offset)
    ptr
  

  def wl_container_of[T <: Listable[T] with Struct[T]](listItemPtr: Pointer[wl_list])(implicit classTag: ClassTag[T]): Pointer[T] =
    wl_container_of(listItemPtr.get)
