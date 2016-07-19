package com.expedia.www.leemajors.spark.streaming.common.util

import com.google.gson.GsonBuilder
import com.expedia.www.leemajors.spark.streaming.common._
import com.expedia.www.leemajors.spark.streaming.common.options.SparkOptions

trait CommandLineOptionsReader[T <: SparkOptions] extends OptionsReader[T] {

  private val gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create

  override def readOptions(args: Array[String]): T = {
    if ((args.length > 1) && args(0).equalsIgnoreCase("-j")) {
      gson.fromJson(args(1), optionsType)
    } else {
      super.readOptions(args)
    }
  }
}
