package com.expedia.www.leemajors.spark.streaming.common.options

/**
  * Created by sogoyal on 7/19/16.
  */
trait ZookeeperOptions {
  val zookeepers: String = null
  val zookeeperConnectionTimeout: Int = 5000
  val zookeeperConnectionRetryInterval: Int = 30000
  val zookeeperNodePath: String = null
}
