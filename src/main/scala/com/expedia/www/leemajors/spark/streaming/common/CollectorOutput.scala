package com.expedia.www.leemajors.spark.streaming.common

import scalaj.http.Http
import com.expedia.www.leemajors.spark.streaming.common.options.CollectorOptions
import scalaj.http.HttpOptions

class CollectorOutput(options: CollectorOptions, result: String) extends AbstractOutput(options, result) {

  def run() {
    try {
      val responseCode = Http(options.collectorEndpoint + options.queueName)
        .postData(result)
        .param("batch", options.batch.toString)
        .param("rollup", options.rollup)
        .param("persist", options.saveToS3.toString)
        .param("stream", options.sendToKafka.toString)
        .header("content-type", "application/json")
        .option(HttpOptions.allowUnsafeSSL)
        .asString.code

      if (responseCode < 300) {
        logInfo(s"Output=${result.length}")
      } else {
        logInfo(s"Not able to send Output=${result.length} to Endpoint=${options.collectorEndpoint}, failed with response = ${responseCode}")
      }

    } catch {
      case ex:Exception => logError(s"Not able to send Output=${result.length} to Endpoint=${options.collectorEndpoint}", ex)
    }

  }
}