package com.expedia.www.leemajors.spark.streaming.common.options

/**
  * Created by sogoyal on 7/19/16.
  */
trait KafkaOptions {
  val kafkaTopic: String = null
  val outputKafkaTopic: String = null
  val clusterID: String = null
}
