package com.expedia.www.leemajors.spark.streaming.common

import scalaj.http.Http
import com.expedia.www.leemajors.spark.streaming.common.options.CollectorOptions
import scalaj.http.HttpOptions

class ConsoleOutput(options: CollectorOptions, result: String) extends AbstractOutput(options, result) {

  def run() {
    try {
      println(s"result=${result.length}")
      println(result)
    } catch {
      case ex: Exception => logError(s"Not able to send [${result}] to endpoint = [${options.collectorEndpoint}]", ex)
    }
  }
}