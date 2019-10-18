package org.freedesktop.wayland

import scala.reflect.ClassTag
import java.foreign.memory.{Pointer,Struct, LayoutType}
import java.foreign.layout.Group
import usr.include.wayland.wayland_util_h.wl_list
import org.freedesktop.wayland.util.Contains
import usr.include.wayland.wayland_server_h.wl_resource


import scala.collection.JavaConverters._

class WlListWrapper[C <: Struct[C]](listPtr: Pointer[wl_list], offset: Long)(given ClassTag[C]) extends Pointer[wl_list]
  export listPtr._

  given Pointer[wl_list] = listPtr
  given Long = offset

  inline def head(): Pointer[C] = WlListMacros.head[C]

  inline def last(): Pointer[C] = WlListMacros.last[C]

  inline def foreach(fn: C => Unit): Unit = WlListMacros.foreach[C](fn)

  inline def length(): Int = WlListMacros.length[C]

  //lazy val length = lengthCalc

  
object WlList
  inline def genOffset[T <: Struct[T]: ClassTag](name: String) = 
    val g = LayoutType.ofStruct(summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]).layout().asInstanceOf[Group]

    val iter = g.elements.iterator
    var cont = true
    var bits = 0l
    while(iter.hasNext && cont)
      val n = iter.next
      if !n.name().filter(_ == name).isEmpty then
        bits += n.bitsSize
      else
        cont = false
    
    -(bits / 8)

  def apply[C <: Struct[C], S <: Singleton & String](listPtr: Pointer[wl_list])(given ClassTag[C], ContainsWlList[C,S]): WlListWrapper[C] = 
    WlListWrapper(listPtr, summon[ContainsWlList[C,S]].offset)

  type ContainsWlList[T <: Struct[T], U <: Singleton & String] = Contains[T,wl_list, U]

  //given Contains[wl_list, wl_list, ""] { val offset = 0 }

  given: (list: Pointer[wl_list]) 
    inline def ofWithName[T <: Struct[T],U <: Singleton & String](given Contains[T, wl_list, U], ClassTag[T]) = WlList(list)
    inline def of[T <: Struct[T]](given Contains[T,wl_list,"link"], ClassTag[T]) = WlList[T,"link"](list)

