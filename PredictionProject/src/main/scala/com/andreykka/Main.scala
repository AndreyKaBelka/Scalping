package com.andreykka

import com.andreykka.dto.KlineEvent
import com.andreykka.redis.RedisAPI
import com.andreykka.service.*
import org.ta4j.core.{BaseBarSeries, BaseTradingRecord}
import zio.*
import zio.logging.*
import zio.logging.backend.SLF4J

case class ServerConfig(host: String, port: Int)

case class DBConfig(name: String)

case class AppConfig(db: DBConfig, serverConfig: ServerConfig)

object Main extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val myApp = for {
    c <- ZIO.service[AppConfig]
    redisApi                <- ZIO.service[RedisAPI]
    processMessagesForKline <- ZIO.service[QueueService]
    res                     <- redisApi
      .subscribeToStream[KlineEvent]("kline", "testGroup", "consumer-1")
      .mapZIO(processMessagesForKline.processMessage)
      .runDrain
      .debug
      .fork
    backtesting             <- ZIO.service[BacktestingService]
    _                       <- backtesting.loadData()
    _ <- ZIO.serviceWithZIO[TrailingGridService](_.initializeGrid(29, 10, 3000))
    _ <- ZIO.serviceWithZIO[TrailingGridService](_.logGrid)
    _ <- ZIO.debug(s"Application started with config: $c")

    _ <- ZIO.never.onInterrupt(ZIO.logInfo("Остановка сервиса"))
  } yield ()

  override def run: ZIO[Any & ZIOAppArgs, Any, Any] =
    myApp
      .provide(
        AppConfig.layer,
        DBConfig.layer,
        ServerConfig.layer,
        RedisAPI.layer,
        QueueService.layer,
        IndicatorService.live,
        ZLayer.succeed {
          val barSeries = new BaseBarSeries("BTCUSDT")
          barSeries.setMaximumBarCount(200)
          barSeries
        },
        ZLayer.succeed {
          new BaseTradingRecord("BTCUSDT")
        },
        RiskManagementService.layer(1000.0),
        BacktestingService.layer,
        TrailingGridService.live,
        OrderBook.live("BTCUSDT"),
      )
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
