package org.freedesktop.wayland

import scala.reflect.ClassTag
import java.foreign.memory.{Pointer,Struct, LayoutType}
import java.foreign.layout.Group
import usr.include.wayland.wayland_util_h.wl_list

import scala.collection.JavaConverters._

case class WlList[C <: Struct[C]](listPtr: Pointer[wl_list], offset: Long)
  inline def head(): Pointer[C] = ${
    WlListMacros.getContainer('{listPtr.get.next$get}, 'offset)
  }

  inline def last(): Pointer[C] = ${
    WlListMacros.getContainer('{listPtr.get.prev$get}, 'offset)
  }

  inline def foreach(fn: C => Unit): Unit = ${
    WlListMacros.foreachImpl[C]('listPtr, 'fn, 'offset)
  }

  inline def length(): Int = ${WlListMacros.lengthImpl[C]('listPtr, 'offset)}

  //lazy val length = lengthCalc

object WlList 
  def apply[C <: Struct[C] : ClassTag](listPtr: Pointer[wl_list]): WlList[C] = 
    WlList(listPtr, offsetOf[C]("list"))

  def offsetOf[T <: Struct[T]](fieldName: String, clazz: Class[T]): Long =
    import scala.language.implicitConversions
    val g = LayoutType.ofStruct(clazz).layout().asInstanceOf[Group]

    val bits = for(l <- g.elements().asScala.takeWhile(_.name().filter(_ == fieldName).isEmpty)) yield
      l.bitsSize()

    -(bits.sum / 8)

  def offsetOf[T <: Struct[T]](fieldName: String)(implicit classTag: ClassTag[T]): Long =
    offsetOf(fieldName,classTag.runtimeClass.asInstanceOf[Class[T]])
  
