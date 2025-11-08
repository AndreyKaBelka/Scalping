package com.andreykka.redis

import com.andreykka.dto.JsonData
import com.andreykka.json.JsonUtils
import zio.json.JsonDecoder
import zio.redis.{CodecSupplier, Redis, RedisError}
import zio.schema.codec.{BinaryCodec, JsonCodec, ProtobufCodec}
import zio.schema.{Schema, StandardType}
import zio.stream.ZStream
import zio.{Task, UIO, ZIO, ZLayer}

case class RedisAPI(redis: Redis) {
  def produceMessage(stream: String, data: JsonData): Task[Option[String]] = for {
    json <- JsonUtils.toJson(data)
    id   <- redis.xAdd(stream, "*")("symbol" -> data.symbol, "data" -> json).returning[String]
  } yield id

  def subscribeToStream[A <: JsonData: JsonDecoder](
    stream: String,
    group: String,
    consumer: String,
  ): ZStream[Any, RedisError, A] =
    ZStream
      .repeatZIOChunk(
        redis
          .xReadGroup(
            group,
            consumer,
            count = Some(10),
          )(stream -> ">")
          .returning[String, String],
      )
      .tapError(err => ZIO.logError(err.toString))
      .mapConcatZIO { result =>
        result.entries.mapZIO { entry =>
          entry.fields.get("data") match {
            case Some(json) =>
              fromJson(stream, group, entry.id, json).option
            case None       =>
              ZIO.logError(s"Missing field: data").as(None)
          }
        }
      }
      .collect { case Some(value) =>
        value
      }

  private def fromJson[A <: JsonData: JsonDecoder](
    stream: String,
    group: String,
    id: String,
    json: String,
  ) = for {
    data <- JsonUtils
      .parseJsonToObject[A](json)
    _    <- redis.xAck(stream, group, id)
  } yield data

  def createConsumerGroup(streamKey: String, consumerGroup: String): Task[Unit] = {
    redis
      .xGroupCreate(streamKey, consumerGroup, "$", mkStream = true)
      .catchSome { case RedisError.BusyGroup(_) =>
        ZIO.logError("Group already exists")
      } *> ZIO.log("Redis group created")
  }
}

object RedisAPI {
  lazy val layer         = (codecSupplierLayer >>> Redis.local) >>> ZLayer.derive[RedisAPI]
  val codecSupplierLayer = ZLayer.succeed {
    new CodecSupplier {
      def get[A: Schema]: BinaryCodec[A] = {
        val schema = implicitly[Schema[A]]

        schema match {
          case Schema.Primitive(StandardType.StringType, _) =>
            ProtobufCodec.protobufCodec[A]

          case _ =>
            JsonCodec.schemaBasedBinaryCodec[A]
        }
      }
    }
  }
}

trait ProcessMessages[A] {
  def processMessage(message: A): Task[Unit]
}
