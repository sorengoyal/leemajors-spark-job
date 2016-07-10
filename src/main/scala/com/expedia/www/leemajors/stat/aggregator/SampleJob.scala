package com.expedia.www.leemajors.stat.aggregator

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import org.apache.spark.streaming.kafka.KafkaUtils
import kafka.serializer.DefaultDecoder
import com.expedia.spark.streaming.common.Output
import com.expedia.spark.streaming.common.StreamingJobRunner
import com.expedia.spark.streaming.common.ThriftDeserializer
import com.expedia.spark.streaming.common.options.SparkOptions
import com.expedia.www.hendrix.signals.definition.SignalData
import com.expedia.www.signal.sdk.model.SpecificSignalReader
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf

object SampleJob extends StreamingJobRunner[Array[Byte], Array[Byte], String, SampleOptions] with ThriftDeserializer {

  override val optionsType = classOf[SampleOptions]

  val signalReader: SpecificSignalReader[SignalData] = new SpecificSignalReader[SignalData](() => new SignalData())

  override def doJob(options: SampleOptions, messages: DStream[String]): Unit = {
    val result = MessageProcessor(this.getClass.getName, messages, Minutes(1), Seconds(10))

    result.foreachRDD { rdd =>
      rdd.foreachPartition(iterator => {
        iterator.foreach(iResult => {
          Output.sendAsCsv(iResult, options)
          /*
          Use this if want to send data directly to kafka, and set outputKafkaTopic to the kafka topic you want to send to
          Also set commons-spark version 1.0.38 or greater for it to work
          Output.sendDirectlyToKafka(iResult.toString(), options)
          */
        })
      })
    }
  }

  override def mapper(input: DStream[(Array[Byte], Array[Byte])]): DStream[String] = {
    input.map(_._2).map(msg => signalReader.deserialize(msg).get.data.get(1).toString)
      /* data has teh following fields :
       * 0 -> EventType
       * 1 -> HotelName
       * 2 -> GUID
       * 3 -> Time
       */
  }

  override def createInputStream(options: SampleOptions, ssc: StreamingContext, storageLevel: StorageLevel)
    (mapper: DStream[(Array[Byte], Array[Byte])] => DStream[String]): DStream[String] = {

    val kafkaParams: Map[String, String] = Map("zookeeper.connect" -> (options.zookeepers + options.zookeeperNodePath),
      "group.id" -> options.jobName,
      "zookeeper.connection.timeout.ms" -> "10000")
    val topicMap = options.kafkaTopic.split(",").map((_, options.numReaderThreads)).toMap
    val kafkaDStreams = (1 to options.numReaderThreads)
      .map { _ => KafkaUtils.createStream[Array[Byte], Array[Byte], DefaultDecoder, DefaultDecoder](ssc, kafkaParams, topicMap, storageLevel)
      }
    mapper(ssc.union(kafkaDStreams).repartition(options.numExecutors))
  }

  class LeeMajorsOptions extends SampleOptions {
    override val batchDuration: Int = 2
    override val zookeepers: String = "zk-watson-1.us-west-2.test.expedia.com:2181"
    override val zookeeperConnectionTimeout: Int = 5000
    override val zookeeperConnectionRetryInterval: Int = 30000
    override val zookeeperNodePath: String = "/kafka/watson"
    override val kafkaTopic: String = "omniture-info-test"
    override val outputKafkaTopic: String = "leemajors"
    override val clusterID: String = "watson"
    override val collectorEndpoint: String = "http://collector.test.expedia.com/"
    override val queueName: String = "sample.json"
    override val batch: Boolean = true
    override val sendToKafka: Boolean = true
    override val saveToS3: Boolean = true
    override val rollup: String = "Minute"
    override val maxTriggeringTimeInMs = 10 * 1000 // 10 seconds
    override val maxTriggeringNumOfMessages = 1000
    override val numReaderThreads: Int = 2
    override val numExecutors: Int = 2
    override val checkPointInS3: Boolean = false
    override val checkPointEnabled: Boolean = true
    override val checkPointFolder: String = "/mnt/ewe-spark-checkpoints/"
    override val jobName: String = "leemajors-spark"
    override val localMode: Boolean = false

  }
  override def main(args: Array[String]): Unit = {

    val options = new LeeMajorsOptions//readOptions(args)

    val checkpointDir = getCheckPointDir(options)
    println("checkpoint dir : " + checkpointDir)
    val storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER

    //    val storageLevel: StorageLevel = options.localMode match {
    //      case true => StorageLevel.MEMORY_AND_DISK_SER
    //      case false => StorageLevel.MEMORY_AND_DISK_SER_2
    //    }
    try {
      println("job name = " + options.jobName)
      logInfo("Starting " + this.getClass.getName + " with options : " + gson.toJson(options))

      val sparkConf: SparkConf = getSparkConf(options)
      val ssc = StreamingContext.getOrCreate(checkpointDir, () => {
        createContext(sparkConf, options.batchDuration, checkpointDir)
      }, new Configuration, true)


      numPartitions = ssc.sparkContext.defaultParallelism
      logInfo("defaultParallelism=" + ssc.sparkContext.defaultParallelism)

      val messages = createInputStream(options, ssc, storageLevel)(mapper)
      doJob(options, messages)

      ssc.start()
      ssc.awaitTermination()

    } catch {
      case ie: InterruptedException => {
        logWarning("Interrupted job : " + this.getClass.getSimpleName)
        throw ie
      }
      case t: Throwable => throw new SparkExecutionException(t)
    }
  }
}