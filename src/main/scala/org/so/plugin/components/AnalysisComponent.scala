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
//          println(PrettyPrinting.prettyTree(showRaw(x)))
          println("function----", y)
          new JoinAnalyzer(lambda, y.toString).transform(x)
          try {
            val usage = la.optimizeLambdaFn(lambda.asInstanceOf[la.global.Tree], y.toString)
            println(usage)
          } catch {
            case e : Exception => println("Can't process this function")
          }
          a
        }
        case a @ q"$x.$y($lambda)" => {
          println("function----", y)
          a
        }
        case _ => super.transform(tree)
      }
    }
  }

  class JoinAnalyzer(val lambda: Tree, val nextFunc: String) extends global.Transformer {
    override def transform(tree: Tree) : Tree = {
//      println(PrettyPrinting.prettyTree(showRaw(tree)))
      tree match {
        case a @ q"rdd.this.RDD.rddToPairRDDFunctions[..$t](..$args)" =>

          // Spark RDD joins reside on the first value of list of Applications in
          // args. Therefore, check whether there's a join inside the first value
          // in args (List).
          if (hasJoin(args.head)) {
            // We have identified a Spark RDD join.
            println("----XXXXX--", args)
            try {
              val usage = la.optimizeLambdaFn(lambda.asInstanceOf[la.global.Tree], nextFunc)
              println(usage)
            } catch {
              case e : Exception => println("Can't process this function")
            }
          }
          a
        case _ => super.transform(tree)
      }
    }

    /**
      * Given a tree, return true if the tree contains a join
      * @param tree
      * @return
      */
    def hasJoin(tree: Tree) : Boolean = {
      tree match {
        case j @ q"$rdd1.join[$typeTree]($rdd2)" =>
          println("Join Found...", j)
          true
        case _ => false
      }
    }
  }
}