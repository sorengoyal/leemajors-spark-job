package com.expedia.www.leemajors.stat.aggregator

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import org.apache.spark.streaming.kafka.KafkaUtils
import com.expedia.spark.streaming.common.Output
import com.expedia.spark.streaming.common.StreamingJobRunner
import com.expedia.spark.streaming.common.ThriftDeserializer
import com.expedia.www.hendrix.signals.definition.SignalData
import com.expedia.www.signal.sdk.model.SpecificSignalReader

object SampleJob extends StreamingJobRunner[Array[Byte], Array[Byte], Array[Byte], SampleOptions] with ThriftDeserializer {

  override val optionsType = classOf[SampleOptions]

  val signalReader: SpecificSignalReader[SignalData] = new SpecificSignalReader[SignalData](() => new SignalData())

  override def doJob(options: SampleOptions, messages: DStream[Array[Byte]]): Unit = {
    //val result = MessageCount(this.getClass.getName, messages, Minutes(1), Seconds(10))

    messages.foreachRDD { rdd =>
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

  override def mapper(input: DStream[(Array[Byte], Array[Byte])]): DStream[SpecficSignal[SignalData]] = {
    input.map(_._2).map(msg =>signalReader.deserialize(msg.value()))
  }

  override def createInputStream(options: SampleOptions, ssc: StreamingContext, storageLevel: StorageLevel)
    (mapper: DStream[(Array[Byte], Array[Byte])] => DStream[Array[Byte]]): DStream[Array[Byte]] = {

    val topicMap = options.kafkaTopic.split(",").map((_, options.numReaderThreads)).toMap
    val kafkaDStreams = (1 to options.numReaderThreads)
      .map { _ => KafkaUtils.createStream(ssc, options.zookeepers + options.zookeeperNodePath, options.jobName, topicMap, storageLevel) }
    mapper(ssc.union(kafkaDStreams).repartition(options.numExecutors))
  }

}