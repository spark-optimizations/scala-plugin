package org.so.plugin

import java.text.SimpleDateFormat

import org.apache.spark.{SparkConf, SparkContext}

/**
  * @author shabbir.ahussain
  */
object BadMapSparkTest {

    def main(args: Array[String]) {
      val conf = new SparkConf().setAppName("Song High Fidelity").setMaster("local")
      val sc = new SparkContext(conf)


      val rdda = sc.parallelize(List((1, (1, 11)), (2, (2, 22))))
      val rddb = sc.parallelize(List((1, (1, 11)), (2, (2, 22))))

      rdda
          .join(rddb)
          .mapValues(x=> x._1._1)
          .collect()
    }
  }
