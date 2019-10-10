package org.freedesktop.wayland

import scala.quoted._
import scala.quoted.matching.searchImplicitExpr
import scala.reflect.ClassTag
import java.foreign.memory.{Pointer,Struct}
import java.foreign.memory.LayoutType
import java.foreign.NativeTypes
import java.foreign.layout.Group
import usr.include.wayland.wayland_util_h.wl_list

object WlListMacros

  trait Contains[T <: Struct[T]: ClassTag, U <: Struct[U], V <: String] {
    val offset = LayoutType.ofStruct(summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]).layout().asInstanceOf[Group]
  }

  inline def head[C <: Struct[C]](given listPtr: Pointer[wl_list], offset: Long): Pointer[C] = ${
    getContainer[C]('{listPtr.get.next$get}, 'offset)
  }

  inline def last[C <: Struct[C]](given listPtr: Pointer[wl_list], offset: Long): Pointer[C] = ${
    getContainer[C]('{listPtr.get.prev$get}, 'offset)
  }

  inline def foreach[C <: Struct[C]](fn: C => Unit)(given listPtr: Pointer[wl_list], offset: Long): Unit = ${
    foreachImpl[C]('listPtr, 'fn, 'offset)
  }

  inline def length[C <: Struct[C]](given listPtr: Pointer[wl_list], offset: Long): Int = ${lengthImpl[C]('listPtr, 'offset)}

  def getContainer[C <: Struct[C]](pointer: Expr[Pointer[wl_list]], offset: Expr[Long])(given Type[C], QuoteContext): Expr[Pointer[C]] = 
    val clazz = searchImplicitExpr[ClassTag[C]] match 
      case Some(ct) => '{$ct.runtimeClass.asInstanceOf[Class[C]]}
      case None => '{???}

    '{$pointer.cast(NativeTypes.VOID).cast(NativeTypes.UINT8).offset($offset).cast(NativeTypes.VOID).cast(LayoutType.ofStruct($clazz))}

  def foreachImpl[C <: Struct[C]](pointer: Expr[Pointer[wl_list]], function: Expr[C => Unit], offset: Expr[Long])(given QuoteContext, Type[C]): Expr[Unit] = '{
    var cur = $pointer.get.next$get
    while(cur != $pointer)
      $function(${getContainer[C](pointer,offset)}.get)
      cur = cur.get.next$get
  }

  def lengthImpl[C <: Struct[C]](pointer: Expr[Pointer[wl_list]], offset: Expr[Long])(given QuoteContext, Type[C]): Expr[Int] = '{
    var length = 0
    ${foreachImpl[C](pointer, '{(_: C) => length += 1}, offset)}
    length
  }