package main.scala.org.so.plugin.components


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

  override val runsAfter: List[String] = List[String]("refchecks")
  override def newTransformer(unit: CompilationUnit) = new Transformer(unit)

  /**
    * Traverses the AST generated right after "refchecks" phase and analyzes
    * usage of columns
    * @param unit AST after "refchecks" phase of scala compiler
    */
  class Transformer(unit: CompilationUnit)
    extends TypingTransformer(unit) {
    override def transform(tree: Tree): Tree = {
      super.transform(tree)
    }
  }
}