package com.andreykka.service

import com.andreykka.DurationImplicits.*
import com.andreykka.dto.KlineEvent
import com.andreykka.redis.ProcessMessages
import izumi.reflect.Tag
import org.ta4j.core.BarSeries
import zio.json.{EncoderOps, JsonEncoder}
import zio.{Task, ZIO, ZLayer}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

case class QueueService(barSeries: BarSeries, indicatorService: IndicatorService) extends ProcessMessages[KlineEvent] {
  def processMessage(message: KlineEvent): Task[Unit] = for {
    _ <- ZIO.unit
    _ <- addBar(message, barSeries)
    _ <- writeToFileStream("json-data.json", message)
  } yield ()

  private def writeToFileStream[A: JsonEncoder](fileName: String, data: A) =
    ZIO.attempt {
      val jsonString = data.toJsonPretty + ","
      Files.write(
        Paths.get("D:\\ideaProjects\\StavkiTraim\\PredictionProject\\src\\main\\resources", fileName),
        jsonString.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.APPEND,
      )
    }
}

object QueueService {
  val layer = ZLayer.derive[QueueService]
}
