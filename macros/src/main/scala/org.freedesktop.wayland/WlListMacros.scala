package org.freedesktop.wayland

import scala.quoted._
import scala.quoted.matching.searchImplicitExpr
import scala.reflect.ClassTag
import java.foreign.memory.{Pointer,Struct}
import java.foreign.memory.LayoutType
import java.foreign.NativeTypes
import usr.include.wayland.wayland_util_h.wl_list

object WlListMacros
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