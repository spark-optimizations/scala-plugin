package org.so.plugin

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.{Transform, TypingTransformers}

class DivByZero(val global: Global) extends Plugin {
  import global._

  val name: String = "divbyzero"
  val description: String = "Description"
  val components: List[PluginComponent ] = List[PluginComponent](Component)

  private object Component
    extends PluginComponent
      with TypingTransformers
      with Transform
  {

    override val global: DivByZero.this.global.type = DivByZero.this.global
    override val runsAfter: List[String] = List[String]("refchecks")
    override val phaseName: String = DivByZero.this.name

    def newPhase(_prev: Phase) = new DivByZeroPhase(_prev)
    def newTransformer(unit: CompilationUnit) = new SetTransformer(unit)

    class DivByZeroPhase(prev: Phase) extends StdPhase(prev) {
      override def name = DivByZero.this.name
      def apply(unit: CompilationUnit) {
        for ( tree @ Apply(Select(rcvr, nme.DIV), List(Literal(Constant(0)))) <- unit.body
              if rcvr.tpe <:< definitions.IntClass.tpe){
          global.reporter.error(tree.pos, "definitely division by zero")

        }
      }
    }

    class SetTransformer(unit: CompilationUnit)
      extends TypingTransformer(unit) {

      override def transform(tree: Tree): Tree = tree match {
        case a@Apply(Select(rcvr, nme.DIV), List(Literal(Constant(0)))) =>
            println("Shab")
            localTyper.typed(treeCopy.Apply(tree, Ident(newTermName("LinkedHashSet")), "shab"))
            println("Shab1")
        }
        case t => super.transform(tree)
      }
    }
  }
}