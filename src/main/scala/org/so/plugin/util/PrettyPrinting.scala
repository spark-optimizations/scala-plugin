package org.so.plugin.util

import scala.reflect.macros.blackbox

object PrettyPrinting {

  /* 
   * Print a raw AST, nicely indented. 
   * Pretty fragile, e.g. will get confused by string literals containing commas or parentheses.
   */
  def prettyTree(raw: String): String = {
    var level = 0
    def indent = "  " * level

    val sparse = raw.map {
      case ',' => s",\n$indent".dropRight(1)
      case '(' =>
        level += 1
        s"(\n$indent"
      case ')' =>
        level -= 1
        s"\n$indent)"
      case other => other
    }.mkString
    sparse.replaceAll("""\(\s+\)""", "()")
  }

  def prettyTree(c: blackbox.Context)(tree: c.universe.Tree): String = 
    prettyTree(c.universe.showRaw(tree))

}