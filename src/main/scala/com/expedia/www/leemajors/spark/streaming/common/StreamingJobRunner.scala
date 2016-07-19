package com.expedia.www.leemajors.spark.streaming.common

//import java.lang.reflect.Array

import com.expedia.www.leemajors.spark.streaming.common.options.StreamingOptions
import com.expedia.www.leemajors.spark.streaming.common.util._
import com.google.gson.Gson
import org.apache.hadoop.conf.Configuration
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.{Logging, SparkConf}

import scala.compat.Platform.currentTime

/**
  * Trait that can be used to create and run a Spark streaming job
  *
  * @param K message key type
  * @param V message value type
  * @param U message type after mapper is run
  * @param T T must be of type StreamingOptions
  */
trait StreamingJobRunner[K, V, U, T <: StreamingOptions] extends CommandLineOptionsReader[T]
  with ClassPathOptionsReader[T] with DefaultOptionsReader[T] with Logging {

  lazy val gson = new Gson
  var numPartitions: Int = 1

  def main(args: Array[String]): Unit = {

    val options = readOptions(args)
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

  def getSparkConf(options: T): SparkConf = {
    val sparkConf = new SparkConf().setAppName(options.jobName)
    if (options.localMode) {
      sparkConf.setMaster("local[*]")
    }
    sparkConf.set("spark.default.parallelism", (options.numExecutors * Runtime.getRuntime.availableProcessors).toString)
    sparkConf.set("spark.speculation", "true")
    sparkConf.set("spark.speculation.interval", "10000")
    sparkConf.set("spark.speculation.multiplier", "4")
    sparkConf.set("spark.speculation.quantile", "0.85")

    sparkConf.set("spark.streaming.backpressure.enabled", "true")
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    sparkConf
  }

  def doJob(options: T, messages: DStream[U]): Unit

  def mapper(input: DStream[(K, V)]): DStream[U]

  def getId(options: T): String = {
    options.jobName + (if (options.localMode) "-local" else "")
  }

  def createInputStream(options: T, ssc: StreamingContext, storageLevel: StorageLevel)(mapper: DStream[(K, V)] => DStream[U]): DStream[U]

  def getCheckPointDir(options: T): String = {
    if (options.checkPointEnabled) {
      if (options.checkPointInS3) {
        return "s3n://" + options.checkPointFolder + "/" + getId(options) + "/" + System.currentTimeMillis() + "/"
      }
      val folderName = options.checkPointFolder + getId(options) + "/" + System.currentTimeMillis()
      println("checkpointing in " + folderName)

      return folderName
    }
    null
  }

  def createContext(sparkConf: org.apache.spark.SparkConf, batchDuration: Int, checkpointDir: String): StreamingContext = {
    val ssc = new StreamingContext(sparkConf, Seconds(batchDuration))
    ssc.checkpoint(checkpointDir)
    ssc
  }

  class SparkExecutionException(e: Throwable) extends RuntimeException(e)

}

abstract class Result(jobName: String, epochMs: Long = currentTime) extends Serializable {

  def toCsv: String = {
    epochMs.toString
  }

  override def toString: String = {
    "epochMs=" + epochMs
  }
}