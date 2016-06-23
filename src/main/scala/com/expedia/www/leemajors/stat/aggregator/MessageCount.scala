package com.expedia.www.leemajors.stat.aggregator

import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.Logging
import org.apache.spark.streaming.Duration
import com.expedia.spark.streaming.common.Result

case class MessageCount(metric: String, count: Long, jobName: String) extends Result(jobName) {

  def this(count: Long, jobName: String) = this("messageCount", count, jobName)

  override def toString() : String = {
    "JobName=" + jobName + ", Metric=" + metric + ", Count=" + count
  }

  override def toCsv() : String = {
    jobName + ", " + metric + ", " + count
  }
  
}

object MessageCount extends Serializable with Logging {

  def apply(jobName: String, messages: DStream[String], windowDuration: Duration, slideDuration: Duration): DStream[MessageCount] = {
    val messageCounts = messages.countByWindow(windowDuration, slideDuration)
      .map(iValue => new MessageCount(iValue, jobName))
    messageCounts
  }
  
} 