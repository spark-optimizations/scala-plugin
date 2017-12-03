package org.so.plugin

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

class JoinOptimizer(val global: Global) extends Plugin {
  override val name = "joinOptimizer"
  override val description = "Optimize Spark join for all actions"
  override val components = List[PluginComponent](Component)

  private object Component extends PluginComponent {
    override val global: Global = JoinOptimizer.this.global
    override val phaseName: String = JoinOptimizer.this.name
    override val runsAfter: List[String] = List[String]("refchecks")

    override def newPhase(prev: Phase): Phase = ???
  }
}