package com.expedia.www.leemajors.spark.streaming.common.util

import com.expedia.www.leemajors.spark.streaming.common._
import com.expedia.www.leemajors.spark.streaming.common.options.SparkOptions

trait DefaultOptionsReader[T <: SparkOptions] extends OptionsReader[T] {

  override def readOptions(args: Array[String]): T = {
    if (args.length == 0) {
      super.readOptions(Array("-r", "sandbox.json"))
    } else {
      super.readOptions(args)
    }
  }
}