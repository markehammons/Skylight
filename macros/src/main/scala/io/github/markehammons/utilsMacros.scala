package io.github.markehammons

import java.foreign.memory.Struct
import java.foreign.memory.LayoutType
import java.foreign.layout.Group

import scala.collection.JavaConverters._
import scala.reflect._


import scala.quoted._

object UtilsMacros
  // def offsetOfImpl[T <: Struct[T]](fieldName: String, structName: String)(given t: Type[T], ct: ClassTag[T])(given QuoteContext): Expr[Long] = 
  //   val g = LayoutType.ofStruct(classOf[T]).layout.asInstanceOf[Group]
  //   val bits = for(l <- g.elements().asScala.takeWhile(_.name().filter(_ == fieldName).isEmpty)) yield 
  //     l.bitsSize

  //   val o = -(bits.sum / 8)
    
  //   inline val offset = o
  //   println(offset)
  //   '{offset}
