package com.andreykka.price

import com.andreykka.Main.isConnected
import com.andreykka.client.BinanceClient
import com.andreykka.dto.{JsonData, KlineData, KlineEvent}
import zio.http.{Client, Request, Response, URL}
import zio.json.DecoderOps
import zio.json.ast.Json
import zio.{RIO, Scope, Task, ZIO, ZLayer}

case class BinanceAPI(binanceClient: BinanceClient) {
  private val wsBaseUrl = URL.decode("wss://stream.binance.com:9443/ws")
  private val apiUrl    = "https://api.binance.com/"

  def webSocketConn(uri: URL): RIO[Client & isConnected, Unit] = ZIO.scoped {
    for {
      client <- ZIO.serviceWith[Client](_.url(uri))
      _      <- client.socket(binanceClient.socketApp()).onInterrupt(ZIO.logInfo("Остановка WebSocket"))
    } yield ()
  }

  def getOHLCVHttp(
    symbol: String,
    interval: String,
    limit: Int = 100,
    startTime: Long,
    endTime: Long,
  ): RIO[Client, Seq[JsonData]] = for {
    _ <- ZIO.unit
    path = "api/v3/klines"
    response <- ZIO.scoped {
      Client
        .streaming(
          Request
            .get(f"$apiUrl/$path")
            .addQueryParam("symbol", symbol)
            .addQueryParam("interval", interval)
            .addQueryParam("limit", limit.toString),
        )
        .flatMap(_.body.asString)
    }
    array    <- ZIO.fromEither(response.fromJson[List[List[Json]]]).catchAll(err => ZIO.logError(err).as(List()))
    klineEvents = array.map { klineEvent =>
      KlineEvent(
        "kline",
        symbol,
        klineEvent.head.asNumber.getOrElse(Json.Num(0L)).value.longValue(),
        KlineData(
          klineEvent(6).asNumber.getOrElse(Json.Num(0L)).value.longValue(),
          klineEvent(1).asString.fold(0d)(_.toDouble),
          klineEvent(2).asString.fold(0d)(_.toDouble),
          klineEvent(3).asString.fold(0d)(_.toDouble),
          klineEvent(4).asString.fold(0d)(_.toDouble),
          klineEvent(5).asString.fold(0d)(_.toDouble),
          interval,
        ),
      )
    }
    _ <- ZIO.log(s"$startTime, $endTime")
  } yield klineEvents

  def sendToWs(data: String): Task[Boolean] = {
    binanceClient.addToQueue(data)
  }

}

object BinanceAPI {
  val layer = ZLayer.derive[BinanceAPI]
}
