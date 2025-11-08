package com.andreykka

import zio.json.{DeriveJsonCodec, DeriveJsonDecoder, JsonCodec, JsonEncoder, jsonField}
import zio.schema.codec.{BinaryCodec, JsonCodec as ZJsonCodec}
import zio.schema.{DeriveSchema, Schema}

package object dto {

  sealed trait JsonData {
    def eventType: String
    def symbol: String
  }

  final case class KlineData(
    @jsonField("T") endTime: Long,
    @jsonField("o") openPrice: Double,
    @jsonField("h") highPrice: Double,
    @jsonField("l") lowPrice: Double,
    @jsonField("c") closePrice: Double,
    @jsonField("v") volume: Double,
    @jsonField("i") duration: String,
  )

  final case class KlineEvent(
    @jsonField("e") `eventType`: String,
    @jsonField("s") `symbol`: String,
    @jsonField("E") eventTime: Long,
    @jsonField("k") klineData: KlineData,
  ) extends JsonData

  case class Position(
    entryPrice: Double,
    stopLoss: Double,
    takeProfit: Double,
    quantity: Double,
    direction: Direction,
  )

  enum Direction {
    case BUY, SELL, HOLD
  }

  object KlineData {
    implicit val schema: Schema[KlineData]       = DeriveSchema.gen
    implicit val jsonCodec: JsonCodec[KlineData] = DeriveJsonCodec.gen
    implicit val codec: BinaryCodec[KlineData]   = ZJsonCodec.zioJsonBinaryCodec(jsonCodec)
  }

  object KlineEvent {
    implicit val schema: Schema[KlineEvent]       = DeriveSchema.gen
    implicit val jsonCodec: JsonCodec[KlineEvent] = DeriveJsonCodec.gen
    implicit val codec: BinaryCodec[KlineEvent]   = ZJsonCodec.zioJsonBinaryCodec(jsonCodec)
  }
}
