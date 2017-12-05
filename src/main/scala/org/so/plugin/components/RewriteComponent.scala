package org.so.plugin.components


import org.so.plugin.analysis.LambdaAnalyzer
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

/**
  * @author Manthan Thakar
  *         
  * This component is responsible for analyzing lambda's used after spark joins to
  * identify columns used. This information then informs column pruning of tables.
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
          // Build a tree for parameters of transformed mapValues functions on RDDs.
          val rdd1Map = getRDDTransformations(usage._1, "a", lambda.asInstanceOf[global.Tree], func.toString)
          val rdd2Map = getRDDTransformations(usage._2, "b", lambda.asInstanceOf[global.Tree], func.toString)
          if (usage._1.contains("-1") && usage._2.contains("-1")) return a
          if (usage._1.contains("-1"))
            return q"$x.join($y.mapValues(b => $rdd2Map)).$func[$ts]($lambda)"
          if (usage._2.contains("-1"))
            return q"$x.mapValues(a => $rdd1Map).join($y).$func[$ts]($lambda)"
          // Return the transformed tree
          return q"$x.mapValues(a => $rdd1Map).join($y.mapValues(b => $rdd2Map)).$func[$ts]($lambda)"
        } catch {
          case e : Exception =>
        }
        a
      }
      case a @ q"$x.join($y).$func($lambda)" => {
        try {
          val usage = la.optimizeLambdaFn(lambda.asInstanceOf[la.global.Tree], func.toString)
          // Build a tree for parameters of transformed mapValues functions on RDDs.
          val rdd1Map = getRDDTransformations(usage._1, "a", lambda.asInstanceOf[global.Tree], func.toString)
          val rdd2Map = getRDDTransformations(usage._2, "b", lambda.asInstanceOf[global.Tree], func.toString)
          if (usage._1.contains("-1") && usage._2.contains("-1")) return a
          if (usage._1.contains("-1"))
            return q"$x.join($y.mapValues(b => $rdd2Map)).$func($lambda)"
          if (usage._2.contains("-1"))
            return q"$x.mapValues(a => $rdd1Map).join($y).$func($lambda)"
          // Return the transformed tree
          return q"$x.mapValues(a => $rdd1Map).join($y.mapValues(b => $rdd2Map)).$func($lambda)"
        } catch {
          case e : Exception =>
        }
        a
      }
      case _ => super.transform(tree)
    }

    /**
      * Given a lambda used right after a join call on RDDs and the name of the function that contains the lambda,
      * return transformations for RDDs
      * @param lambda
      * @param func
      * @return
      */
    def getRDDTransformations(usageLookup: Set[String], termName: String,
                              lambda: Tree, func: String): Tree = {
      val params = getParamNodes(usageLookup, termName)
      Apply(Select(Ident("scala"), TermName("Tuple22")), params)
    }

    /**
      * Given a usage lookup for an RDD's column, return Select node if a column is in the lookup,
      * otherwise return Literal `null`
      * @param usageLookup a set containing accessors on a RDD, depicting the columns used
      * @param termName a dummy termname for the `Select` node
      * @return
      */
    def getParamNodes(usageLookup: Set[String], termName: String) : List[Tree] = {
      var params = List[Tree]()
      for (i <- 22 to 1 by -1) {
        if (usageLookup.contains("_" + i))
          params = Select(Ident(TermName(termName)), TermName("_" + i)) :: params
        else
          params = Literal(Constant(null)) :: params
      }
      params
    }
  }
}