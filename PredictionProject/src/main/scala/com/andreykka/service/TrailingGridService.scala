package com.andreykka.service

import com.andreykka.dto.OrderSide.{BUY, SELL}
import com.andreykka.dto.OrderType.LIMIT
import com.andreykka.dto.SelfTradePreventionMode.NONE
import com.andreykka.dto.{OrderDTO, OrderRequest}
import zio.{UIO, ZIO, ZLayer}

import java.time.Instant
import scala.util.Random

case class TrailingGridService(orderBook: OrderBook) {
  def initializeGrid(grids: Int, leverage: Int, currentPrice: Double): UIO[Unit] = for {
    _ <- ZIO.foreachDiscard(1 to grids) { level =>
      val buyOrder  = buildBuyOrder(currentPrice - level * 30)
      val sellOrder = buildSellOrder(currentPrice + level * 30)
      orderBook.addOrder(sellOrder) *> orderBook.addOrder(buyOrder)
    }
  } yield ()

  private def buildBuyOrder(price: Double) =
    OrderDTO(
      "BTCUSDT",
      Random.nextLong(),
      -1,
      Random.nextString(12),
      Instant.now(),
      price,
      0.01,
      0.01,
      0.01,
      0.01,
      "FILLED",
      "GTC",
      LIMIT,
      BUY,
      Instant.now(),
      NONE,
    )

  private def buildSellOrder(price: Double) =
    OrderDTO(
      "BTCUSDT",
      Random.nextLong(),
      -1,
      Random.nextString(12),
      Instant.now(),
      price,
      0.01,
      0.01,
      0.01,
      0.01,
      "FILLED",
      "GTC",
      LIMIT,
      SELL,
      Instant.now(),
      NONE,
    )

  def logGrid = for {
    _ <- ZIO.unit
    orders = orderBook.getBuyOrders ++ orderBook.getSellOrders
    _ <- ZIO.log(orders.map(_.price).toString())
  } yield ()

  def adjustGrid(orderCompleted: OrderDTO) = for {
    _ <- ZIO.unit
    
  } yield ()

  private def isActivateOrder(currentPrice: Double) = for {
    _ <- ZIO.unit
  } yield ()

}

object TrailingGridService {
  val live = ZLayer.derive[TrailingGridService]
}
