package org.so.plugin.components

import org.so.plugin.analysis.LambdaAnalyzer
import org.so.plugin.util.PrettyPrinting

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

/**
  * This component is responsible for analyzing lambda's used after spark joins to
  * identify columns used. This information later on informs column pruning of tables.
  * @param global
  * @param phaseName
  */
class AnalysisComponent(val global: Global, val phaseName: String) extends PluginComponent
  with TypingTransformers
  with Transform {
  import global._

  override val runsAfter: List[String] = List[String]("refchecks")
  override def newTransformer(unit: CompilationUnit) = new Transformer(unit)
  val la = new LambdaAnalyzer(global)

  /**
    * Traverses the AST generated right after "refchecks" phase and analyzes
    * usage of columns
    * @param unit AST after "refchecks" phase of scala compiler
    */
  class Transformer(unit: CompilationUnit)
    extends TypingTransformer(unit) {
    override def transform(tree: Tree): Tree = {
      tree match {
        case a @ q"$x.$y[$t]($lambda)" => {
          new TreeTraverser().traverse(x)
          try {
            val usage = la.optimizeLambdaFn(lambda.asInstanceOf[la.global.Tree], y.toString)
            println(usage)
          } catch {
            case e : Exception => println("Can't process this function")
          }
          a
        }
        case _ => super.transform(tree)
      }
    }
  }

  class TreeTraverser extends Traverser {
    override def traverse(tree: Tree) : Unit = {
      tree match {
        case q"$rdd1.join[$tpt]($rdd2)" =>
          println("----XXXXX--", rdd1, showRaw(rdd2))
        case a =>
//          println("----", a)
          super.traverse(tree)
      }
    }
  }
}