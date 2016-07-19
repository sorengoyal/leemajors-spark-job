package com.expedia.www.leemajors.spark.streaming.common.options

/**
  * Created by sogoyal on 7/19/16.
  */
trait CollectorOptions {
  val collectorEndpoint: String = null
  val queueName: String = null
  val batch: Boolean = true
  val sendToKafka: Boolean = true
  val saveToS3: Boolean = false
  val rollup: String = "Hour"
  val maxTriggeringTimeInMs = 10 * 1000 // 10 seconds
  val maxTriggeringNumOfMessages = 1000
}
