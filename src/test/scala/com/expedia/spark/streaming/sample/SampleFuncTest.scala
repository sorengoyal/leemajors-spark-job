package com.expedia.www.leemajors.stat.aggregator.tests


import com.expedia.www.leemajors.stat.aggregator.MessageProcess
import com.google.gson.Gson
import org.apache.spark.streaming.{Seconds, TestSuiteBase}
import org.apache.spark.streaming.dstream.DStream

class SampleFuncTest extends TestSuiteBase {

  override def actuallyWait: Boolean = false

  override def numInputPartitions: Int = 1

  override def maxWaitTimeMillis: Int = 30 * 1000

  // 30 seconds
  val gson: Gson = new Gson
  val dataFile = "src/test/data/leemajors.jsonData"

  test("Sample Message Processing test case") {

    val inputSeq = List.fill(3)(messagesGenerator)

    val ssc = setupStreams[String, MessageProcess](inputSeq,
      (inputDStream: DStream[String]) => {
        MessageProcess(this.getClass.getName, inputDStream, Seconds(2), Seconds(1))
      })

    val output: Seq[Seq[MessageProcess]] = runStreams[MessageProcess](ssc, 1, 5)

    output.foreach(messages => {
      assert(!messages.isEmpty)

      messages.foreach(message => {
        println(message)
      })
    })

    //    output.last.foreach {
    //      case (messageCount) =>
    //        assert(messageCount.count == 0)
    //    }
    //  }
  }

  def messagesGenerator(): scala.collection.mutable.Queue[String] = {
    val inputData = scala.collection.mutable.Queue[String]()
    inputData ++= scala.io.Source.fromFile(dataFile).getLines().toSeq
    inputData
  }
}