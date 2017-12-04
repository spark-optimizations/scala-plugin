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
        case a @ q"rdd.this.RDD.rddToPairRDDFunctions[..$t](..$args)(..$tags).$y[$ts]($lambda)" => {
//          println(PrettyPrinting.prettyTree(showRaw(x)))
          println("function----", y)
//          println("XXXX", showRaw(args))
          new JoinAnalyzer(lambda, y.toString).transform(args.head)
          a
        }
        // This case matches `map` followed by join
        // For some reason, map followed by join has one less level of nesting of
        // `rdd.this.RDD.rddToPairRDDFunctions`, which renders the previous case useless
        case a @ q"$x.$y[$t]($lambda)" => {
          println("function----", y, "\n", PrettyPrinting.prettyTree(showRaw(x)))
          new JoinAnalyzer(lambda, y.toString).transform(x)
          a
        }
        // This case matches `filter` followed by join
        // In theory, the previous pattern should match these. But filter functions
        // don't have `TypeApply` nodes, since they aren't generic.
        case a @ q"$x.$y($lambda)" => {
//          println("function----", y, "\n", PrettyPrinting.prettyTree(showRaw(x)))
          val transformed = new JoinAnalyzer(lambda, y.toString).transform(x)
          println("After trans", q"$transformed.$y($lambda)")
          a
        }
        case _ => super.transform(tree)
      }
    }
  }

  class JoinAnalyzer(val lambda: Tree, val nextFunc: String) extends global.Transformer {
    override def transform(tree: Tree) : Tree = {
      tree match {
        // Find the join function call as well as target RDDs on which join is called.
        case a @ q"rdd.this.RDD.rddToPairRDDFunctions[..$t](..$rdd1)(..$tags).join[$tpt]($rdd2)" =>
          println(rdd1, rdd2)

          // Attempt to obtain the columns used for both RDDs involved in join
          try {
            val usage = la.optimizeLambdaFn(lambda.asInstanceOf[la.global.Tree], nextFunc)
            println(usage)
          } catch {
            case e : Exception => println("Can't process this function")
          }
          a
        case _ => super.transform(tree)
      }
    }
  }
}