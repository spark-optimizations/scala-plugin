package org.so.plugin.analysis

import scala.tools.nsc.Global

class LambdaAnalyzer(val global: Global) {
  val RDDTrans = Set("mapValues", "map")

  import global._

  /** Finds parameter usage for function. Will return none if following substructure is not found in the given tree from root.
    * "Function(List(ValDef(_, TermName(<param>), _, _)), _) "
    *
    * @param tree   is the function body tree to parse.
    * @param fnName is the spark RDD transformation to optimize for.
    * @return A tuple of fields used in left and right side of tuple. If empty then none of the fields are used from that tuple.
    */
  def optimizeLambdaFn(tree: Tree, fnName: String)
  : (Set[String], Set[String]) = {
    if (!RDDTrans.contains(fnName))
      throw new Exception("Unsupported spark transformation : " + fnName)

    tree match {
      case Function(List(ValDef(_, TermName(param), _, _)), fnBody) =>
        findParamUsage(fnBody, param, fnName)
    }
  }

  /** Finds the usages of the given parameter.
    *
    * @param tree   is the function body tree to parse.
    * @param param  is the parameter to scan for.
    * @param fnName is the spark RDD transformation to optimize for.
    * @return A tuple of fields used in left and right side of tuple. If empty then none of the fields are used from that tuple.
    */
  private def findParamUsage(tree: Tree, param: String, fnName: String)
  : (Set[String], Set[String]) = {
    /** Finds the usages of the given parameter and tuple position.
      *
      * @param tree is the function body tree to parse.
      * @param pos  is the position to look for within tuple.
      */
    def fposMapValues(tree: Tree, pos: String): Set[String] = tree match {
      case Select(Select(Ident(TermName(`param`)), TermName(`pos`)), TermName(idx)) =>  Set(idx)
      case Select(Ident(TermName(`param`)), TermName(`pos`)) => Set("-1")
      case Select(Ident(TermName(`param`)), TermName(_)) => Set()
      case Ident(TermName(`param`)) => Set("-2")
      case a =>
        a.children
          .flatMap(x => fposMapValues(x, pos))
          .toSet
    }

    /** Finds the usages of the given parameter and tuple position.
      *
      * @param tree is the function body tree to parse.
      * @param pos  is the position to look for within tuple.
      */
    def fposMap(tree: Tree, pos: String): Set[String] = tree match {
      case Select(Select(Select(Ident(TermName(`param`)), TermName("_2")), TermName(`pos`)), TermName(idx)) => Set(idx)
      case Select(Select(Ident(TermName(`param`)), TermName("_2")), TermName(`pos`)) => Set("-1")
      case Select(Select(Ident(TermName(`param`)), TermName("_2")), TermName(_)) => Set()
      case Select(Ident(TermName(`param`)), TermName("_2")) => Set("-2")
      case a =>
        a.children
          .flatMap(x => fposMap(x, pos))
          .toSet
    }

    fnName match {
      case "mapValues" => (fposMapValues(tree, "_1"), fposMapValues(tree, "_2"))
      case "map"  => (fposMap(tree, "_1"), fposMap(tree, "_2"))
    }
  }
}
