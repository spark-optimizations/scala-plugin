package org.so.plugin

import org.so.plugin.util._

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.{Transform, TypingTransformers}

class ShabTest(val global: Global) extends Plugin {
  import global._

  val name: String = "divbyzero"
  val description: String = "Description"
  val components: List[PluginComponent ] = List[PluginComponent](Component)

  private object Component
    extends PluginComponent
      with TypingTransformers
      with Transform
  {

    override val global: ShabTest.this.global.type = ShabTest.this.global
    override val runsAfter: List[String] = List[String]("refchecks")
    override val phaseName: String = ShabTest.this.name

//    def newPhase(_prev: Phase) = new DivByZeroPhase(_prev)
    override def newTransformer(unit: CompilationUnit) = new MyTransformer(unit)

    class DivByZeroPhase(prev: Phase) extends StdPhase(prev) {
      override def name:String = ShabTest.this.name
      def apply(unit: CompilationUnit) {
        global.reporter.error(unit.body.pos, "definitely division by zero")

        for ( tree @ Apply(Select(rcvr, nme.DIV), List(Literal(Constant(0)))) <- unit.body if rcvr.tpe <:< definitions.IntClass.tpe){
            global.reporter.error(tree.pos, "definitely division by zero")


        }
      }
    }

    class MyTransformer(unit: CompilationUnit)
      extends TypingTransformer(unit) {
      override def transform(tree: Tree): Tree = {
        println(PrettyPrinting.prettyTree(showRaw(tree)))

        tree match {
          case Apply(_,_) =>  println("Found")
          case _ =>
            super.transform(tree)

        }


        tree
      }
//        tree match {
//        case a@Apply(Select(rcvr, nme.DIV), args) =>
//            println("Shab")
//            localTyper.typed(treeCopy.Apply(tree, Ident(newTermName("LinkedHashSet")), args))
//        case t => super.transform(tree)
//      }
    }
  }
}