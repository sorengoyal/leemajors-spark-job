package com.expedia.www.leemajors.spark.streaming.common

import com.expedia.www.commons.kafka.producer.Producer

/**
  * Created by sogoyal on 13/04/16.
  */
object KafkaProducer {
  var producer: Producer = null
  //
  def getProducer(zkHosts: String, clusterId: String, topicName: String): Producer = {
    if (producer == null) {
      producer = new Producer(zkHosts, clusterId, topicName)
      producer.initializeConnection()
    }
    producer
  }

  def closeProducer(): Unit = {
    producer.closeConnection()
    producer = null
  }
}
