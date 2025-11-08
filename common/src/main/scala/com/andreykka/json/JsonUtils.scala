package com.andreykka.json

import com.andreykka.dto.{JsonData, KlineEvent}
import zio.json.ast.{Json, JsonCursor}
import zio.json.{DecoderOps, JsonDecoder}
import zio.{Task, ZIO}

object JsonUtils {
  private val eventType = JsonCursor.field("e") >>> JsonCursor.isString
  private val symbol    = JsonCursor.field("s") >>> JsonCursor.isString

  def parseJson(data: String): Task[JsonData] = for {
    eventTypeData <- ZIO.succeed(data.fromJson[Json].flatMap(_.get(eventType).map(_.value)))
    json          <- ZIO
      .fromEither(eventTypeData.flatMap {
        case "kline" => data.fromJson[KlineEvent]
        case _       => Left("Unknown event type")
      })
      .mapError(RuntimeException(_))
  } yield json

  def parseJsonToObject[A <: JsonData: JsonDecoder](data: String): Task[A] = for {
    json <- ZIO
      .fromEither(data.fromJson[A])
      .mapError(RuntimeException(_))
  } yield json

  def toJson(data: JsonData): Task[String] = for {
    _    <- ZIO.unit
    json <- data.eventType match {
      case "kline" => ZIO.succeed(KlineEvent.jsonCodec.encodeJson(data.asInstanceOf[KlineEvent], None))
      case _       => ZIO.fail(RuntimeException("Cant encode"))
    }
  } yield json.toString

}
