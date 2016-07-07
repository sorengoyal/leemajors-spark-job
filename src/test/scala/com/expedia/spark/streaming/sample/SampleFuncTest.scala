package com.expedia.www.leemajors.stat.aggregator.tests


import com.expedia.www.leemajors.stat.aggregator.MessageProcessor
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

    val ssc = setupStreams[String, MessageProcessor](inputSeq,
      (inputDStream: DStream[String]) => {
        MessageProcessor(this.getClass.getName, inputDStream, Seconds(2), Seconds(1))
      })

    val output: Seq[Seq[MessageProcessor]] = runStreams[MessageProcessor](ssc, 1, 1)
    var count = 0
    output.foreach(messages => {
      assert(!messages.isEmpty)
      messages.foreach(message => {
        count += 1
        assert(message.data == "Hotel" + count.toString + ",1")
        logInfo(message.data)
      })
    })
    output.last.foreach {
      case (message) =>
        assert(!message.data.isEmpty)
    }
  }

  def messagesGenerator(): scala.collection.mutable.Queue[String] = {
    val inputData = scala.collection.mutable.Queue[String]()
    inputData ++= scala.io.Source.fromFile(dataFile).getLines().toSeq
    //inputData.foreach(println(_))
    inputData
  }
}