package com.expedia.www.leemajors.spark.streaming.common.options

class StreamingOptions extends SparkOptions with Serializable {

  val batchDuration: Int = 60
}