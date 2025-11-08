package com.andreykka.service

import org.ta4j.core.*
import org.ta4j.core.AnalysisCriterion.PositionFilter
import org.ta4j.core.criteria.pnl.ReturnCriterion
import org.ta4j.core.criteria.{PositionsRatioCriterion, ReturnOverMaxDrawdownCriterion, VersusEnterAndHoldCriterion}
import org.ta4j.core.indicators.*
import org.ta4j.core.indicators.bollinger.BollingerBandFacade
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.*
import zio.{Task, UIO, ZIO, ZLayer}

case class IndicatorService(
  barSeries: BarSeries,
) {

  def macdIndicator = {
    new MACDIndicator(closePriceIndicator)
  }

  def bollingerBands = {
    new BollingerBandFacade(barSeries, 30, 2)
  }

  def rsiIndicator = {
    new RSIIndicator(closePriceIndicator, 14)
  }

  private def closePriceIndicator = {
    new ClosePriceIndicator(barSeries)
  }

  def macdSignal(macdIndicator: MACDIndicator) = {
    new EMAIndicator(macdIndicator, 9)
  }

  def smaIndicator = {
    new SMAIndicator(closePriceIndicator, 200)
  }

  def ema50Indicator = {
    new EMAIndicator(closePriceIndicator, 50)
  }

  def atrIndicator = {
    new ATRIndicator(barSeries, 14)
  }
}

object IndicatorService {
  val live = ZLayer.derive[IndicatorService]
}
