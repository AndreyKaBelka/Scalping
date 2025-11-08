import com.andreykka.dto.KlineEvent
import com.andreykka.redis.RedisAPI
import zio.logging.backend.SLF4J
import zio.{Runtime, ULayer, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

case class ServerConfig(host: String, port: Int)

case class DBConfig(name: String)

case class AppConfig(db: DBConfig, serverConfig: ServerConfig)

object Main extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val myApp = for {
    c        <- ZIO.service[AppConfig]
    redisApi <- ZIO.service[RedisAPI]
    res      <- redisApi
      .subscribeToStream[KlineEvent]("kline", "testGroup", "consumer-1")
      .mapZIO(line => ZIO.logInfo(line.toString))
      .runDrain
      .debug
      .fork
    _        <- ZIO.debug(s"Application started with config: $c")
    _        <- ZIO.never.onInterrupt(ZIO.logInfo("Остановка сервиса"))
  } yield ()

  override def run: ZIO[Any & ZIOAppArgs, Any, Any] =
    myApp
      .provide(
        AppConfig.layer,
        DBConfig.layer,
        ServerConfig.layer,
        RedisAPI.layer,
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
