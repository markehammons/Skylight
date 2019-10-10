package org.freedesktop.wayland.util

import java.foreign.memory.Struct
import scala.reflect.ClassTag
import java.foreign.layout.Group
import java.foreign.memory.{LayoutType, Pointer, Struct}
import usr.include.wayland.wayland_server_h.wl_resource
import usr.include.wayland.wayland_util_h.wl_list


trait Contains[T <: Struct[T]: ClassTag, U <: Struct[U], V <: String]
  val offset: Long

  def genOffset(name: V) = 
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
  
