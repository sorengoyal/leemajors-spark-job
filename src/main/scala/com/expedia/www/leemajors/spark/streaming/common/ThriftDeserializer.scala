package com.expedia.www.leemajors.spark.streaming.common

import com.google.gson.Gson
import org.apache.thrift.protocol.TJSONProtocol
import org.apache.thrift.{ TBase, TDeserializer, TFieldIdEnum }

trait ThriftDeserializer {

  val jsonSerializer: Gson = new Gson

  def toJson[T <: TBase[_ <: TBase[_, _], _ <: TFieldIdEnum]](message: Array[Byte], typeOfMessage: Class[T]): String = {

    jsonSerializer.toJson(toObject(message, typeOfMessage))
  }

  def toObject[T <: TBase[_ <: TBase[_, _], _ <: TFieldIdEnum]](message: Array[Byte], typeOfMessage: Class[T]): T = {

    val deserializer: TDeserializer = new TDeserializer(new TJSONProtocol.Factory())

    val messageBase = typeOfMessage.newInstance()
    deserializer.deserialize(messageBase, message)

    messageBase
  }
}