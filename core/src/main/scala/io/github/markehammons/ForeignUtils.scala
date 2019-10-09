package io.github.markehammons

import java.foreign.Scope
import java.foreign.memory.Struct
import java.foreign.memory.LayoutType
import scala.reflect.ClassTag

def allocateStruct[T <: Struct[T]](clazz: Class[T])(given scope: Scope): T = scope.allocateStruct(clazz)
  
def allocateArray[T](layoutType: LayoutType[T], size: Long)(given scope: Scope) = scope.allocateArray(layoutType,size)

def allocateCallback[T](callback: T)(given scope: Scope) = scope.allocateCallback(callback)