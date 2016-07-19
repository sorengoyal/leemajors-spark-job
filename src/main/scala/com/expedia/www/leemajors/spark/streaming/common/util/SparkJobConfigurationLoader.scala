package com.expedia.www.leemajors.spark.streaming.common.util

import com.google.gson.{ JsonArray, JsonElement, JsonObject, JsonParser }
import scala.collection.JavaConverters._

object SparkJobConfigurationLoader {

  private val OPTIONS_ROOT = "options"
  private val CONFIG_FILE = "config.json"
  private val FILE_PATH_SEPARATOR = "/"

  private val parser = new JsonParser

  def loadConfigurations(optionsFileName: String, jobName: String): String = {
    val config = loadConfig(optionsFileName)
    config.addProperty("jobName", jobName)
    config.toString
  }

  def loadConfigurations(optionsFileName: String): String = {
    loadConfig(optionsFileName).toString
  }

  private def loadConfig(optionsFileName: String): JsonObject = {
    val baseConfig = new JsonObject
    val jobConfig = parseClassPathConfig(optionsFileName)

    applyConfig(baseConfig, jobConfig)

    baseConfig
  }

  private def parseClassPathConfig(fileName: String): JsonObject = {
    println("Options : " + OPTIONS_ROOT + FILE_PATH_SEPARATOR + fileName)
    val json: String = scala.io.Source.fromInputStream(
      getClass.getClassLoader.getResourceAsStream(OPTIONS_ROOT + FILE_PATH_SEPARATOR + fileName)).mkString
    parser.parse(json).asInstanceOf[JsonObject]
  }

  private def applyConfig(baseConfig: JsonObject, overridingConfig: JsonObject): Unit = {
    overridingConfig.entrySet().asScala.collect({
      case entry: java.util.Map.Entry[String, JsonElement] => {
        if (entry.getValue.isJsonPrimitive) {
          baseConfig.add(entry.getKey, entry.getValue)
        } else if (entry.getValue.isJsonObject) {
          Option(baseConfig.getAsJsonObject(entry.getKey)) match {
            case Some(obj) => applyConfig(obj, entry.getValue.getAsJsonObject)
            case None => {
              baseConfig.add(entry.getKey, new JsonObject)
              applyConfig(baseConfig, overridingConfig)
            }
          }
        } else if (entry.getValue.isJsonArray) {
          Option(baseConfig.getAsJsonArray(entry.getKey)) match {
            case Some(obj) => obj.addAll(entry.getValue.getAsJsonArray)
            case None => {
              val obj: JsonArray = new JsonArray
              baseConfig.add(entry.getKey, obj)
              obj.addAll(entry.getValue.getAsJsonArray)
            }
          }
        }
      }
    })
  }
}
