package com.expedia.www.leemajors.spark.streaming.common.options

trait SparkOptions {
  val numReaderThreads: Int = 1
  val numExecutors: Int = 1
  val checkPointInS3: Boolean = false
  val checkPointEnabled: Boolean = true
  val checkPointFolder: String = "ewe-spark-checkpoints/"
  val jobName: String = null
  val localMode: Boolean = false
}