package org.so.plugin

object DivByZeroTest {
    val five = 5
    val amount = five / 0
    def main(args: Array[String]) {
      println(amount)
    }
}
