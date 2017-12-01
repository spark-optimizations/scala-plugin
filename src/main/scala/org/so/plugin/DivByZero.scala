package org.so.plugin

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

class DivByZero(val global: Global) extends Plugin {
  import global._

  val name: String = "divbyzero"
  val description: String = "checks for division by zero"
  val components: List[PluginComponent ] = List[PluginComponent](Component)

  private object Component extends PluginComponent {
    val global: DivByZero.this.global.type = DivByZero.this.global
    val runsAfter: List[String ] = List[String]("refchecks")
    override val phaseName: String = DivByZero.this.name

    def newPhase(_prev: Phase) = new DivByZeroPhase(_prev)

    class DivByZeroPhase(prev: Phase) extends StdPhase(prev) {
      override def name = DivByZero.this.name
      def apply(unit: CompilationUnit) {
        for ( tree @ Apply(Select(rcvr, nme.DIV), List(Literal(Constant(0)))) <- unit.body
              if rcvr.tpe <:< definitions.IntClass.tpe)
        {
          global.reporter.error(tree.pos, "definitely division by zero")
        }
      }
    }
  }
}