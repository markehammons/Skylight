package org.freedesktop.wayland.util

import java.foreign.memory.Struct
import scala.collection.immutable.Set
import scala.collection.immutable.Map

trait ContainsLists[T <: Struct[T]](listNames: Set[String])