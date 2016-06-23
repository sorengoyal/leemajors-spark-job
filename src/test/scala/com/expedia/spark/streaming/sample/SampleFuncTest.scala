package com.expedia.www.spark.streaming.sample

import com.expedia.www.leemajors.stat.aggregator.MessageCount
import com.google.gson.Gson
import org.apache.spark.streaming.{Seconds, TestSuiteBase}
import org.apache.spark.streaming.dstream.DStream

class SampleFuncTest extends TestSuiteBase {

  override def actuallyWait: Boolean = false
  override def numInputPartitions: Int = 1
  override def maxWaitTimeMillis: Int = 30 * 1000 // 30 seconds
  val gson: Gson = new Gson
  val dataFile = "src/test/data/small-uis.jsonData"

  test("Sample UIS message count test case") {

    val inputSeq = List.fill(3)(uisMessagesGenerator)

    val ssc = setupStreams[String, MessageCount](inputSeq,
      (inputDStream: DStream[String]) => {
        MessageCount(this.getClass.getName, inputDStream, Seconds(2), Seconds(1))
      })

    val output: Seq[Seq[MessageCount]] = runStreams[MessageCount](ssc, 5, 5)

    var count = 0
    output.foreach(messageCounts => {
      count += 1
      assert(!messageCounts.isEmpty)

      messageCounts.foreach(messageCount => {
        if (count == 5) {
          assert(messageCount.count == 0)
        } else if (count == 1 || count == 4) {
          assert(messageCount.count == 46)
        } else {
          assert(messageCount.count == 92)
        }
        logInfo(count + ") " + messageCount)
      })
    })

    output.last.foreach {
      case (messageCount) =>
        assert(messageCount.count == 0)
    }
  }

  def uisMessagesGenerator(): scala.collection.mutable.Queue[String] = {
    val inputData = scala.collection.mutable.Queue[String]()
    inputData ++= scala.io.Source.fromFile(dataFile).getLines().toSeq
    inputData
  }

}