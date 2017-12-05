package org.so.plugin.components


import org.so.plugin.analysis.LambdaAnalyzer
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

/**
  * This component is responsible for analyzing lambda's used after spark joins to
  * identify columns used. This information later on informs column pruning of tables.
  * @param global
  * @param phaseName
  */
class RewriteComponent(val global: Global, val phaseName: String) extends PluginComponent
  with TypingTransformers
  with Transform {
  import global._

  override val runsAfter: List[String] = List[String]("parser")
  override def newTransformer(unit: CompilationUnit) = new Transformer(unit)
  val la = new LambdaAnalyzer(global)
  /**
    * Traverses the AST generated right after "parser" phase and analyzes
    * usage of columns
    * @param unit AST after "parser" phase of scala compiler
    */
  class Transformer(unit: CompilationUnit)
    extends TypingTransformer(unit) {
    override def transform(tree: Tree): Tree = tree match {
      // Look for a join pattern
      case a @ q"$x.join($y).$func[$ts]($lambda)" => {
        try {
          val usage = la.optimizeLambdaFn(lambda.asInstanceOf[la.global.Tree], func.toString)
          var params1 = List[Tree]() // Stores the parameters for transformations on RDD1
          var params2 = List[Tree]() // Stores the parameters for transformations on RDD2
          for (i <- 22 to 1 by -1) {
            params1 = getParamNode(usage._1, "a", i) :: params1
            params2 = getParamNode(usage._2, "b", i) :: params2
          }
          // Build a tree for parameters of transformed mapValues functions on RDDs.
          val rdd1Map = Apply(Select(Ident("scala"), TermName("Tuple22")), params1)
          val rdd2Map = Apply(Select(Ident("scala"), TermName("Tuple22")), params2)
          // Return the transformed tree
          return q"$x.mapValues(a => $rdd1Map).join($y.mapValues(b => $rdd2Map)).$func[$ts]($lambda)"
        } catch {
          case e : Exception =>
        }
        a
      }
      case _ => super.transform(tree)
    }

    /**
      * Given a usage lookup for an RDD's column, return Select node if a column is in the lookup,
      * otherwise return Literal `null`
      * @param usageLookup a set containing accessors on a RDD, depicting the columns used
      * @param termName a dummy termname for the `Select` node
      * @param i column number
      * @return
      */
    def getParamNode(usageLookup: Set[String], termName: String, i : Int) : Tree = {
      if (usageLookup.contains("_" + i))
        Select(Ident(TermName(termName)), TermName("_"+i))
      else
        Literal(Constant(null))

    }
  }
}