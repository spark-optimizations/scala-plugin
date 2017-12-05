package test.scala.org.so.plugin

import org.apache.spark.{SparkConf, SparkContext}

/**
  * @author Manthan Thakar
  */
class BadMapSparkTest_1_NoTypeHint {

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Bad Test 1")
      .setMaster("local")
    val sc = new SparkContext(conf)

    val rdda = sc.parallelize(List((1, (1, 11)), (2, (2, 22))))
    val rddb = sc.parallelize(List((1, (1, 11)), (2, (2, 22))))

    rdda
      .join(rddb)
      .mapValues(x=> x._1._1 + x._2._2)
      .collect()
  }
}