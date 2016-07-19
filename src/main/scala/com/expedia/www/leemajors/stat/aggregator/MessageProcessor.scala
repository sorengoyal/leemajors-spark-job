package com.expedia.www.leemajors.stat.aggregator

import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.Logging
import org.apache.spark.streaming.Duration
import com.expedia.www.leemajors.spark.streaming.common.Result

case class MessageProcessor(metric: String, data: String, jobName: String) extends Result(jobName) {

  def this(data: String, jobName: String) = this("messageCount", data, jobName)

  override def toString() : String = {
    "JobName=" + jobName + ", Metric=" + metric + ", Count=" + data
  }

  override def toCsv() : String = {
    jobName + ", " + metric + ", " + data
  }

}

object MessageProcessor extends Serializable with Logging {

  def apply(jobName: String, messages: DStream[String], windowDuration: Duration, slideDuration: Duration): DStream[MessageProcessor] = {
    val messageStream = messages.map(msg => new MessageProcessor(msg + ",1", jobName))
    messageStream
  }
}
