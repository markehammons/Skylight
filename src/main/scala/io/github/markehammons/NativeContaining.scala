package io.github.markehammons

import java.foreign.Scope
import scala.ref.PhantomReference
import scala.ref.ReferenceQueue
import scala.collection.immutable.Map

trait NativeContaining(given Scope)
  given (given scope: Scope): Scope = scope.fork
  NativeContainings.register(this)

object NativeContainings
  private var refMap = Map.empty[PhantomReference[_],Scope]
  private val referenceQueue = new ReferenceQueue
  def register[T <: AnyRef](myself: T)(given scope: Scope): Unit = 
    referenceQueue.synchronized{
      val pr = PhantomReference(myself, referenceQueue)
      refMap = refMap + (pr -> scope)
    }