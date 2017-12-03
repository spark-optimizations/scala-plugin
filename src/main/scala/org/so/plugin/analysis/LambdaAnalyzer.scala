package org.so.plugin.analysis

import org.so.plugin
import org.so.plugin.ShabTest

import scala.tools.nsc.Global

class LambdaAnalyzer(val global: Global) {
  import global._

  /** Finds parameter usage for mapValues lambda.
    *
    * @param tree is the function body tree to parse.
    * @return A tuple of fields used in left and right side of tuple. If empty then none of the fields are used from that
    */
  def findUsage(tree: Tree): Option[(Set[String], Set[String])] = tree match {
    case Apply(TypeApply(Select(_, TermName("mapValues")), _), List(f)) =>
      optimizeFunction(f)
    case a =>
      a.children
        .foreach(x=> {
          val res = findUsage(x)
          if (res.isDefined) return res
        })
      None
  }

  /** Finds parameter usage for function. Will return none if following substructure is not found in the given tree from root.
    *   "Function(List(ValDef(_, TermName(param), _, _)), _) "
    *
    * @param tree is the function body tree to parse.
    * @return A tuple of fields used in left and right side of tuple. If empty then none of the fields are used from that tuple.
    */
  def optimizeFunction(tree: Tree): Option[(Set[String], Set[String])] = tree match {
    case Function(List(ValDef(_, TermName(param), _, _)), _) =>
      println(param)
      Some(findUsages(tree, param))
    case _=> None
  }


  /** Finds the usages of the given parameter.
    *
    * @param tree  is the function body tree to parse.
    * @param param is the parameter to scan for.
    * @return A tuple of fields used in left and right side of tuple. If empty then none of the fields are used from that tuple.
    */
  private def findUsages(tree: Tree, param: String)
  : (Set[String], Set[String]) = {

    /** Finds the usages of the given parameter and
      *
      * @param tree is the function body tree to parse.
      * @param pos  is the position to look for within tuple.
      */
    def findFirstTerm(tree: Tree, pos: String): Set[String] = tree match {
      case Select(Select(Ident(TermName(`param`)), TermName(`pos`)), a@TermName(idx)) =>
        Set(idx)
      case a =>
        a.children
          .flatMap(x => findFirstTerm(x, pos))
          .toSet
    }

    (findFirstTerm(tree, "_1"), findFirstTerm(tree, "_2"))
  }
}