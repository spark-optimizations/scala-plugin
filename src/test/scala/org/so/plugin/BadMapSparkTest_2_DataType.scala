package org.so.plugin

import org.apache.spark.{SparkConf, SparkContext}

/**
  * @author shabbir.ahussain
  */
object BadMapSparkTest_2_DataType {
    def main(args: Array[String]) {
      val conf = new SparkConf().setAppName("Bad Test 2")
        .setMaster("local")
      val sc = new SparkContext(conf)

      val rdda = sc.parallelize(List((1, (1, 11)), (2, (2, 22))))
      val rddb = sc.parallelize(List((1, (1, 11)), (2, (2, 22))))

      rdda
          .join(rddb)
          .mapValues[Int]{(x:((Int, Int), (Int, Int)))
              => x._1._1 + x._2._2}
          .collect()
    }
  }
