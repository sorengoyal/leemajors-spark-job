package com.expedia.www.leemajors.stat.aggregator

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import org.apache.spark.streaming.kafka.KafkaUtils
import kafka.serializer.DefaultDecoder
import com.expedia.www.leemajors.spark.streaming.common.Output
import com.expedia.www.leemajors.spark.streaming.common.StreamingJobRunner
import com.expedia.www.leemajors.spark.streaming.common.ThriftDeserializer
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
}