import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.transform.{ Transform, TypingTransformers }

object Compiler {
  val settings = new Settings(sys.error)
  val reporter = new ConsoleReporter(settings)
  val compiler = new Global(settings, reporter)

  def main(args: Array[String]) : Unit = {
    val run = new compiler.Run
    run.compile("../src/Test.scala" :: Nil)
    for (unit <- run.units) {
      println("*********** %s ***********".format(unit))
      println(unit.body)
    }
  }
}