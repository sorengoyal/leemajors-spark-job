package com.expedia.www.leemajors.spark.streaming.common


import akka.io.Tcp.Message
import org.apache.spark.Logging
import com.expedia.www.leemajors.spark.streaming.common.options.CollectorOptions
import com.expedia.www.leemajors.spark.streaming.common.options.KafkaOptions
import com.expedia.www.leemajors.spark.streaming.common.options.ZookeeperOptions
import com.google.gson.Gson
import scalaj.http.HttpOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.LongAdder
import com.expedia.www.commons.kafka.producer.Producer
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.RecordMetadata
import java.util.concurrent.Future

object Output extends Logging with Serializable {

  val UPPER_INTERVAL: Long = 2000
  val LOWER_INTERVAL: Long = 1000
  lazy val gson = new Gson
  val queue = new scala.collection.mutable.SynchronizedQueue[String]
  val resultBuffer = new StringBuilder
  val counter: LongAdder = new LongAdder()
  val pool = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors)
  @volatile var lastTick = System.currentTimeMillis()
  @volatile var dequeing = false
  @volatile var interval: Long = LOWER_INTERVAL

  def sendAsJson(result: Result, options: CollectorOptions) = {
    if (options.batch) {
      if (!dequeing && System.currentTimeMillis() - lastTick > interval) {
        sendBatch(options)
        lastTick = System.currentTimeMillis()
      }
      queue += gson.toJson(result)
    } else {
      send(gson.toJson(result), options)
    }

  }

  def sendAsCsv(result: Result, options: CollectorOptions) = {
    send(result.toCsv, options)
  }

  def send(item: String, options: CollectorOptions) {
    pool.execute(getOutputer(options, item))
  }

  def send(items: scala.collection.mutable.Queue[String], options: CollectorOptions) {
    if (items.isEmpty) return
    logInfo(s"items=${items.length}")
    send(items.dequeueAll { _.nonEmpty }.mkString("[", ",", "]"), options)
  }

  def sendBatch(options: CollectorOptions) {
    resultBuffer.synchronized {
      dequeing = true
      while (queue.nonEmpty) {
        val items = new scala.collection.mutable.Queue[String]
        while (queue.nonEmpty && items.length < options.maxTriggeringNumOfMessages) {
          items += queue.dequeue()
        }
        if (items.length >= options.maxTriggeringNumOfMessages)
          interval = LOWER_INTERVAL
        else
          interval = UPPER_INTERVAL
        send(items, options)
      }
      dequeing = false
    }

  }

  def sendDirectlyToKafka[T <: KafkaOptions with ZookeeperOptions](message: String, options: T) : Future[RecordMetadata] = {
    val producer: Producer = KafkaProducer.getProducer(options.zookeepers, options.clusterID, options.outputKafkaTopic)
    producer.postMessage(message)
  }

  def sendDirectlyToKafka[T <: KafkaOptions with ZookeeperOptions](message: String, options: T, callback: Callback) : Future[RecordMetadata] = {
    val producer: Producer = KafkaProducer.getProducer(options.zookeepers, options.clusterID, options.outputKafkaTopic)
    producer.postMessage(message, callback)
  }

  def closeConnectionToKafka(): Unit = {
    KafkaProducer.closeProducer()
  }

  def getOutputer(options: CollectorOptions, result: String): AbstractOutput = {
    if (options.sendToKafka || options.saveToS3) {
      new CollectorOutput(options, result)
    } else {
      new ConsoleOutput(options, result)
    }

  }

}

abstract class AbstractOutput(options: CollectorOptions, result: String) extends Runnable with Logging {
}