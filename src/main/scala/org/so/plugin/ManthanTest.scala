package org.so.plugin

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.{Transform, TypingTransformers}

class ManthanTest(val global: Global) extends Plugin {
  import global._
  val name = "divbyzero"
  val description = "checks for division by zero"
  val components = List[PluginComponent](Component)

  private object Component extends PluginComponent with TypingTransformers with Transform {
    val global: ManthanTest.this.global.type = ManthanTest.this.global
    val runsAfter: List[String] = List[String]{"parser"}
    val phaseName: String = ManthanTest.this.name
    def newPhase(_prev: Phase) = new DivByZeroPhase(_prev)

    class DivByZeroPhase(prev: Phase) extends StdPhase(prev) {
      override def name = ManthanTest.this.name
      def apply(unit: CompilationUnit) {
        unit.body = new PackageTransformer(unit).transform(unit.body)
      }
    }

    class PackageTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
      override def transform(tree: Tree): Tree = tree match {
        case q"println($value)" =>
          println(value)
          q"System.out.println($value)"
        case _ => super.transform(tree)
      }
    }

    def newTransformer(unit: CompilationUnit) =
      new PackageTransformer(unit)
  }
}