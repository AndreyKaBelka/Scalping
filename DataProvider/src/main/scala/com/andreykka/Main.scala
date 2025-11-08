package com.andreykka

import com.andreykka.client.BinanceClient
import com.andreykka.dto.JsonData
import com.andreykka.price.BinanceAPI
import com.andreykka.redis.RedisAPI
import zio.http.{Client, URL}
import zio.logging.backend.SLF4J
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer, *}

import java.time.Instant

case class ServerConfig(host: String, port: Int)

case class DBConfig(name: String)

case class AppConfig(db: DBConfig, serverConfig: ServerConfig)

object Main extends ZIOAppDefault {
  type isConnected = Promise[Throwable, Unit]
  private lazy val wsBaseUrl = URL.decode("wss://stream.binance.com:9443/ws")

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val isConnectedLayer: ULayer[isConnected] =
    ZLayer.fromZIO(Promise.make[Throwable, Unit])

  private val myApp = for {
    c           <- ZIO.service[AppConfig]
    bApi        <- ZIO.service[BinanceAPI]
    isConnected <- ZIO.service[isConnected]
    _           <- ZIO.logInfo(isConnected.hashCode().toString)
    uri         <- ZIO.fromEither(wsBaseUrl)
    redisApi    <- ZIO.service[RedisAPI]
    _           <- redisApi
                     .createConsumerGroup("kline", "testGroup")
                     .tapError(err => ZIO.logError(err.toString))
    _           <- bApi.webSocketConn(uri).forkDaemon
    _           <- isConnected.await
    _           <- ZIO.logInfo("ПОДКЛЮЧИЛСЯЯЯЯ")
    _           <- bApi.sendToWs(
                     "{\"method\": \"SUBSCRIBE\",\"params\": [\"btcusdt@kline_1s\"],\"id\": null}"
                   )
    _           <- ZIO.debug(s"Application started with config: $c")
    _           <- ZIO.never.onInterrupt(ZIO.logInfo("Остановка сервиса"))
  } yield ()

  override def run: ZIO[Any & ZIOAppArgs, Any, Any] =
    myApp
      .provide(
        isConnectedLayer,
        AppConfig.layer,
        DBConfig.layer,
        ServerConfig.layer,
        Client.default,
        BinanceAPI.layer,
        BinanceClient.layer,
        RedisAPI.layer,
      )

  private def fetchLastTwoDaysKlines(symbol: String, interval: String) = {
    val now        = Instant.now().toEpochMilli
    val twoDaysAgo = now - (30 * 24 * 60 * 60 * 1000) // 2 дня в миллисекундах
    val step       = 1000 * 60 * 1000L                // 1000 минут в мс (API лимит)

    def loop(start: Long, acc: Seq[JsonData]): ZIO[Client & BinanceAPI, Throwable, Seq[JsonData]] =
      if (start >= now) ZIO.succeed(acc)
      else {
        val nextEnd = Math.min(start + step, now)
        ZIO.serviceWithZIO[BinanceAPI](
          _.getOHLCVHttp(symbol, interval, limit = 1000, start, nextEnd).flatMap { newKlines =>
            loop(nextEnd, acc ++ newKlines)
          }
        )
      }

    loop(twoDaysAgo, List.empty)
  }

}

object ServerConfig {
  val layer: ULayer[ServerConfig] = ZLayer.succeed(ServerConfig("localhost", 8080))
}

object DBConfig {
  val layer: ULayer[DBConfig] = ZLayer.succeed(DBConfig("my-test-db"))
}

object AppConfig {

  val layer: ZLayer[DBConfig & ServerConfig, Nothing, AppConfig] = ZLayer {
    for {
      db     <- ZIO.service[DBConfig]
      server <- ZIO.service[ServerConfig]
    } yield AppConfig(db, server)
  }

}
