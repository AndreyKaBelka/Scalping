package com.andreykka.service

import zio.{Ref, Task, UIO, ZIO, ZLayer}

case class RiskManagementService(balance: Ref[Double], dailyLoss: Ref[Double], riskPerTrade: Double = 0.01) {
  private val maxDrawdown    = balance.get.map(_ * 0.1)  // Максимальная просадка 10%
  private val dailyLossLimit = balance.get.map(_ * 0.05) // Лимит дневного убытка 5%

  def getBalance: UIO[Double] = balance.get

  def getDailyLoss: UIO[Double] = dailyLoss.get

  def updateBalance(profit: Double): Task[Unit] = for {
    _ <- balance.update(_ + profit)
    _ <- dailyLoss.update(_ + profit)
  } yield ()
}

object RiskManagementService {
  def layer(currentBalance: Double, dailyLoss: Double = 0.0) = ZLayer {
    for {
      balance   <- Ref.make(currentBalance)
      dailyLoss <- Ref.make(dailyLoss)
    } yield new RiskManagementService(balance, dailyLoss)
  }
}
