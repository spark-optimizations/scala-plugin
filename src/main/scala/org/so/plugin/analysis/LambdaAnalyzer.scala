package org.so.plugin.analysis

import scala.tools.nsc.Global

class LambdaAnalyzer(val global: Global) {
  val RDDTrans = List("mapValues", "map",  "filter")

  import global._

  /** Finds parameter usage for mapValues lambda.
    * "Apply(TypeApply(Select(_, TermName(<SparkFunction>)), _), List(<lambda>))"
    *
    * @param tree is the function body tree to parse.
    * @return A tuple of fields used in left and right side of tuple. If empty then none of the fields are used from that.
    */
  @throws(classOf[Exception])
  def findOptimalParms(tree: Tree): (Set[String], Set[String]) = {

    def matchSparkFn(tree: Tree, fnType : String)
    : Option[(Set[String], Set[String])] = tree match {
      case Apply(TypeApply(Select(_, TermName(`fnType`)), _), List(f)) =>
        Some(optimizeLambdaFn(f, fnType))
      case _ => None
    }

    RDDTrans.foreach(x=> {
      val res = matchSparkFn(tree, x)
      if (res.isDefined) return res.get
    })

    throw new Exception("Unsupported spark transformation")
  }

  /** Finds parameter usage for function. Will return none if following substructure is not found in the given tree from root.
    * "Function(List(ValDef(_, TermName(<param>), _, _)), _) "
    *
    * @param tree is the function body tree to parse.
    * @param fnType is the speark RDD transformation to optimize for.
    * @return A tuple of fields used in left and right side of tuple. If empty then none of the fields are used from that tuple.
    */
  private def optimizeLambdaFn(tree: Tree, fnType: String)
  : (Set[String], Set[String]) = tree match {
    case Function(List(ValDef(_, TermName(param), _, _)), _) =>
      findParamUsage(tree, param, fnType)
  }


  /** Finds the usages of the given parameter.
    *
    * @param tree  is the function body tree to parse.
    * @param param is the parameter to scan for.
    * @param fnType is the speark RDD transformation to optimize for.
    * @return A tuple of fields used in left and right side of tuple. If empty then none of the fields are used from that tuple.
    */
  private def findParamUsage(tree: Tree, param: String, fnType: String)
  : (Set[String], Set[String]) = {

    /** Finds the usages of the given parameter and tuple position.
      *
      * @param tree is the function body tree to parse.
      * @param pos  is the position to look for within tuple.
      */
    def findParamPosUsage(tree: Tree, pos: String): Set[String] = tree match {
      case Select(Select(Ident(TermName(`param`)), TermName(`pos`)), a@TermName(idx)) =>
        Set(idx)
      case a =>
        a.children
          .flatMap(x => findParamPosUsage(x, pos))
          .toSet
    }
    (findParamPosUsage(tree, "_1"), findParamPosUsage(tree, "_2"))
  }
}
