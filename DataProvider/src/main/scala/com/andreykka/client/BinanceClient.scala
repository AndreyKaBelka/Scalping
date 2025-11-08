package com.andreykka.client

import com.andreykka.Main.isConnected
import com.andreykka.redis.{RedisAPI, RedisUtil}
import com.codahale.metrics.{MetricRegistry, Timer}
import zio.*
import zio.http.*
import zio.http.ChannelEvent.Read
import zio.http.ChannelEvent.UserEvent.HandshakeComplete
import zio.interop.catz.*

case class BinanceClient(
  messageQueue: Queue[WebSocketFrame],
  isConnected: isConnected,
  redisAPI: RedisAPI,
) {
  private val metrics             = new MetricRegistry()
  private val requestTimer: Timer = metrics.timer("http-request-timer")
  private val pubDataBaseUrl      = "https://data-api.binance.vision"
  private val userDataBaseUrl     = "https://api.binance.com"
  private val wsBaseUrl           = URL.decode("wss://stream.binance.com:9443/ws")

  def socketApp(): WebSocketApp[Scope] = Handler.webSocket { channel =>
    for {
      _ <- processQueue(channel)
      _ <- handleWebSocketMessages(channel)
    } yield ()
  }

  private def handleWebSocketMessages(channel: WebSocketChannel) = {
    channel.receiveAll {
      case ChannelEvent.UserEventTriggered(HandshakeComplete) =>
        ZIO.log("✅ Подключение установлено!") *> isConnected.succeed(()) *> ZIO.logInfo(isConnected.hashCode().toString)
      case Read(WebSocketFrame.Text(data))                    =>
        ZIO.log(s"📥 Получены данные: $data") *> parseAndPublish(data)
      case Read(WebSocketFrame.Ping)                          =>
        channel.send(Read(WebSocketFrame.Pong)) *> ZIO.log("🔄 Пинг -> Понг")
      case _                                                  =>
        ZIO.unit
    }
  }

  private def parseAndPublish(data: String) = RedisUtil
    .getDataForStream(data)
    .tapError(err => ZIO.logError(s"❌ Ошибка парсинга JSON: $err"))
    .flatMap { case (stream, _, data) =>
      redisAPI.produceMessage(stream, data).flatMap(id => ZIO.log(s"✅ JSON добавлен в поток $stream: $id"))
    }
    .catchAll(err => ZIO.log(err.toString))
    .ignore

  private def processQueue(channel: WebSocketChannel) = {
    for {
      _    <- ZIO.log("Z TUUUTTAAAA")
      data <- messageQueue.take
      _    <- channel.send(Read(data))
    } yield ()
  }.forever.forkScoped.onInterrupt(ZIO.logInfo("Останавливаем обработку очереди")).unit

  def addToQueue(data: String): Task[Boolean] = {
    ZIO.log(data) *>
      messageQueue.offer(WebSocketFrame.text(data))
  }

}

object BinanceClient {
  val layer = ZLayer.derive[BinanceClient]
}
