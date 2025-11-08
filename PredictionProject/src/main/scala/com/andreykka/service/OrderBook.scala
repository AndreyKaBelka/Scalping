package com.andreykka.service

import com.andreykka.dto.OrderDTO
import com.andreykka.dto.OrderSide.{BUY, SELL}
import zio.{UIO, ZIO, ZLayer}

import java.util.concurrent.ConcurrentSkipListMap
import scala.jdk.CollectionConverters.*

case class OrderBook(symbol: String) {
  private val buyOrders       = new ConcurrentSkipListMap[BigDecimal, OrderDTO](Ordering[BigDecimal].reverse)
  private val sellOrders      = new ConcurrentSkipListMap[BigDecimal, OrderDTO](Ordering[BigDecimal])
  private val orderBookSymbol = symbol

  def addOrder(order: OrderDTO): UIO[Unit] = for {
    _ <- ZIO.unit
    _ = order.side match
      case BUY  => buyOrders.put(order.price, order)
      case SELL => sellOrders.put(order.price, order)
  } yield ()

  def removeOrder(orderId: Long): UIO[Boolean] = for {
    _ <- ZIO.unit
    removedFromBuy = buyOrders.values().removeIf(_.orderId == orderId)
    removedFromSell = sellOrders.values().removeIf(_.orderId == orderId)
  } yield removedFromBuy || removedFromSell

  def getBuyOrders: Seq[OrderDTO] = {
    buyOrders.values().asScala.toSeq
  }

  def getSellOrders: Seq[OrderDTO] = {
    sellOrders.values().asScala.toSeq
  }

  def getClosestBuyOrder(price: Double): Option[OrderDTO] = {
    Option(buyOrders.floorEntry(price)).map(_.getValue)
  }

  def getClosestSellOrder(price: Double): Option[OrderDTO] = {
    Option(sellOrders.ceilingEntry(price)).map(_.getValue)
  }
}

object OrderBook {
  def live(symbol: String) = ZLayer.succeed(new OrderBook(symbol))
}
