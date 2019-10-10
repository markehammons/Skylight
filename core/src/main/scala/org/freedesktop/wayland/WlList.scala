package org.freedesktop.wayland

import scala.reflect.ClassTag
import java.foreign.memory.{Pointer,Struct, LayoutType}
import java.foreign.layout.Group
import usr.include.wayland.wayland_util_h.wl_list
import org.freedesktop.wayland.util.Contains
import usr.include.wayland.wayland_server_h.wl_resource


import scala.collection.JavaConverters._

case class WlList[C <: Struct[C]](listPtr: Pointer[wl_list], offset: Long)

  given Pointer[wl_list] = listPtr
  given Long = offset
  inline def head(): Pointer[C] = WlListMacros.head[C]

  inline def last(): Pointer[C] = WlListMacros.last[C]

  inline def foreach(fn: C => Unit): Unit = WlListMacros.foreach[C](fn)

  inline def length(): Int = WlListMacros.length[C]

  //lazy val length = lengthCalc

object WlList 
  def apply[C <: Struct[C], S <: String](listPtr: Pointer[wl_list])(given ClassTag[C], ContainsWlList[C,S]): WlList[C] = 
    WlList(listPtr, summon[ContainsWlList[C,S]].offset)

  def offsetOf[T <: Struct[T]](fieldName: String, clazz: Class[T]): Long =
    import scala.language.implicitConversions
    val g = LayoutType.ofStruct(clazz).layout().asInstanceOf[Group]

    val bits = for(l <- g.elements().asScala.takeWhile(_.name().filter(_ == fieldName).isEmpty)) yield
      l.bitsSize()

    -(bits.sum / 8)

  def offsetOf[T <: Struct[T]](fieldName: String)(implicit classTag: ClassTag[T]): Long =
    offsetOf(fieldName,classTag.runtimeClass.asInstanceOf[Class[T]])
  
  type ContainsWlList[T <: Struct[T], U <: String] = Contains[T,wl_list, U]

  given ContainsWlList[wl_resource, "link"] = new ContainsWlList[wl_resource, "link"] { val offset = genOffset("link") }
