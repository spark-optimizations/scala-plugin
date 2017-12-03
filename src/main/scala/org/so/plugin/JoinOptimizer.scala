package main.scala.org.so.plugin

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import components.AnalysisComponent

class JoinOptimizer(val global: Global) extends Plugin {

  override val name = "JoinOptimizer"
  override val description = "Optimize Spark joins for all actions"
  val analysisComponent = new AnalysisComponent(global, name)
  override val components = List(analysisComponent)
}