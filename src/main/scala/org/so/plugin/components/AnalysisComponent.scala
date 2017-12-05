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
          val ctx = new JoinContext(t, args, tags)
//          println(ctx.getRDDLengths)
          val trans = new JoinAnalyzer(lambda, y.toString, ctx).transform(args.head)
          println("Transformed", trans)
          a
        }
        // This case matches `map` followed by join
        // For some reason, map followed by join has one less level of nesting of
        // `rdd.this.RDD.rddToPairRDDFunctions`, which renders the previous case useless
        case a @ q"$x.$y[$t]($lambda)" => {
//          println("function----", y, "\n", PrettyPrinting.prettyTree(showRaw(x)))
          a
        }
        // This case matches `filter` followed by join
        // In theory, the previous pattern should match these. But filter functions
        // don't have `TypeApply` nodes, since they aren't generic.
        case a @ q"$x.$y($lambda)" => {
//          val transformed = new JoinAnalyzer(lambda, y.toString).transform(x)
//          println("After trans", q"$transformed.$y($lambda)")
          a
        }
        case _ => super.transform(tree)
      }
    }
  }

  class JoinAnalyzer(val lambda: Tree,
                     val nextFunc: String,
                     val joinCtx: JoinContext) extends global.Transformer {
    override def transform(tree: Tree) : Tree = {
      tree match {
        // Find the join function call as well as target RDDs on which join is called.
        case a @ q"rdd.this.RDD.rddToPairRDDFunctions[..$t](..$rdd1)(..$tags).join[$tpt]($rdd2)" =>
          println("rdds----", rdd1.head, rdd2)
          println("join return type----", t)
          // Attempt to obtain the columns used for both RDDs involved in join
          try {
            val usage = la.optimizeLambdaFn(lambda.asInstanceOf[la.global.Tree], nextFunc)
            val usedIndices = {
              (usage._1.map(getIndexFromAccessor), usage._2.map(getIndexFromAccessor))
            }
            println("Used ", usedIndices)
            val rddTypes = joinCtx.getRDDTypes
            val replacedTypes = {
              (rddTypes._1.zipWithIndex.map(x => if (usedIndices._1.contains(x._2 + 1)) x._1 else null),
              rddTypes._2.zipWithIndex.map(x => if (usedIndices._2.contains(x._2 + 1)) x._1 else null))
            }
            println("Replaced Types", replacedTypes)
            val newTypes = List(joinCtx.getJoinKeyType, replacedTypes)
            val args = joinCtx.joinBody
            return q"rdd.this.RDD.rddToPairRDDFunctions[$replacedTypes]"
          } catch {
            case e : Exception => println("Can't process this function", e)
          }
          a
        case _ => super.transform(tree)
      }
    }

    def getIndexFromAccessor(a : String): Int = a.split("_")(1).toInt
  }

  class JoinContext(val joinReturnType: List[Tree],
                    val joinBody: List[Tree],
                    val pairRDDTags: List[Tree]) {

    def getRDDTypes() : (List[Type], List[Type]) = {
//      joinReturnType(1).tpe = List((Int, Int), (Int, Int))
      val joinValueReturnTypes = joinReturnType(1)
      (joinValueReturnTypes.tpe.typeArgs(0).typeArgs,
        joinValueReturnTypes.tpe.typeArgs(1).typeArgs)
    }

    def getJoinKeyType(): Tree = joinReturnType.head
  }
}