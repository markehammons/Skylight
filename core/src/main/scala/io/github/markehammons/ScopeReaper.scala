package io.github.markehammons

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, ActorContext, AbstractBehavior}
import java.foreign.Scope

object ScopeReaper
  def apply(): Behavior[Scope] = Behaviors.setup( context => new ScopeReaper(context) )

class ScopeReaper(context: ActorContext[Scope]) extends AbstractBehavior[Scope] 
  override def onMessage(scope: Scope) = ???