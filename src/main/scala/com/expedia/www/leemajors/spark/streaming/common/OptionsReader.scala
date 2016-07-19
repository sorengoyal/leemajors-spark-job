package com.expedia.www.leemajors.spark.streaming.common

/**
  * Created by sogoyal on 7/19/16.
  */
import com.expedia.www.leemajors.spark.streaming.common.options.SparkOptions

trait OptionsReader[T <: SparkOptions] {
  val optionsType: Class[T]

  /**
    * Implement this method to read the options object from command
    * line arguments
    * options
    * @param args arguments provided in command line arguments
    * @return Instance of T
    */
  def readOptions(args: Array[String]): T = throw new FailedReadingOptions("Invalid options.")

  class FailedReadingOptions(msg: String) extends RuntimeException(msg)
}

