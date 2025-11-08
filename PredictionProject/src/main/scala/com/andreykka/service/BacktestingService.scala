package com.andreykka.service

import com.andreykka.DurationImplicits.*
import com.andreykka.dto.KlineEvent
import org.ta4j.core.BarSeries
import zio.json.{DecoderOps, JsonDecoder}
import zio.{Task, ZIO, ZLayer}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

case class BacktestingService(indicatorService: IndicatorService, baseBarSeries: BarSeries) {

  def loadData() = for {
    klines <- readJsonArrayFromResources[KlineEvent]("json-data.json")
    _      <- ZIO.foreachDiscard(klines)(kline => addBar(kline, baseBarSeries))
    _      <- ZIO.log(baseBarSeries.getSeriesPeriodDescription)
  } yield ()

  private def readJsonArrayFromResources[A: JsonDecoder](fileName: String): Task[List[A]] = {
    val path = Paths.get("D:\\ideaProjects\\StavkiTraim\\PredictionProject\\src\\main\\resources", fileName)
    ZIO.attempt {
      val jsonString = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
      jsonString.fromJson[List[A]] match {
        case Right(obj)  => obj
        case Left(error) => throw new RuntimeException(s"Ошибка десериализации JSON: $error")
      }
    }
  }

  def backtestStrategy() = for {
    _ <- ZIO.unit
  } yield ()
}

object BacktestingService {
  val layer = ZLayer.derive[BacktestingService]
}
