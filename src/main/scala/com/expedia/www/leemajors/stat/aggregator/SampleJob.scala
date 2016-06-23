package com.expedia.www.leemajors.stat.aggregator

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{ Minutes, Seconds, StreamingContext }
import org.apache.spark.streaming.dstream.{ DStream, ReceiverInputDStream }
import org.apache.spark.streaming.kafka.KafkaUtils

import com.expedia.spark.streaming.common.Output
import com.expedia.spark.streaming.common.StreamingJobRunner
import com.expedia.spark.streaming.common.ThriftDeserializer
import com.expedia.www.user.interaction.v1.UserInteraction

object SampleJob extends StreamingJobRunner[String, String, String, SampleOptions] with ThriftDeserializer {

  override val optionsType = classOf[SampleOptions]

  override def doJob(options: SampleOptions, messages: DStream[String]): Unit = {
    val result = MessageCount(this.getClass.getName, messages, Minutes(1), Seconds(10))

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

  override def mapper(input: DStream[(String, String)]): DStream[String] = {
    input.map(_._2).map(i => toJson(i.getBytes, classOf[UserInteraction]))
  }

  override def createInputStream(options: SampleOptions, ssc: StreamingContext, storageLevel: StorageLevel)
    (mapper: DStream[(String, String)] => DStream[String]): DStream[String] = {

    val topicMap = options.kafkaTopic.split(",").map((_, options.numReaderThreads)).toMap
    val kafkaDStreams = (1 to options.numReaderThreads)
      .map { _ => KafkaUtils.createStream(ssc, options.zookeepers + options.zookeeperNodePath, options.jobName, topicMap, storageLevel) }
    mapper(ssc.union(kafkaDStreams).repartition(options.numExecutors))
  }

}