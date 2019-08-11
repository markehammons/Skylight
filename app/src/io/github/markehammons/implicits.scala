package io.github.markehammons

import java.foreign.memory.Pointer

object implicits {
  implicit case object WlrBackendHasExtractableEventsInstance extends WlrBackendHasExtractableEvents
  implicit case object WlrOutputHasExtractableEventsInstance extends WlrOutputHasExtractableEvents

  implicit def PointersToAnonExtractablesHaveExtractableAnon[T,U](implicit extractable: HasExtractableEvents[T,U]): HasExtractableEvents[Pointer[T],U] = new HasExtractableEvents[Pointer[T],U] {
    override def extractFrom(tPointer: Pointer[T]): U = extractable.extractFrom(tPointer.get())
  }
}
