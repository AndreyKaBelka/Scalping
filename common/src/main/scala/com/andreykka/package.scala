package com

import com.andreykka.dto.KlineEvent
import org.ta4j.core.{BarSeries, BaseBar}
import zio.ZIO

import java.time.{Duration, ZoneOffset, ZonedDateTime}
import scala.language.implicitConversions

package object andreykka {
  object DurationImplicits {

    implicit def stringToDuration(input: String): Duration = {
      if (input.endsWith("s")) {
        val seconds = input.substring(0, input.length - 1).toLong
        Duration.ofSeconds(seconds)
      } else if (input.endsWith("m")) {
        val minutes = input.substring(0, input.length - 1).toLong
        Duration.ofMinutes(minutes)
      } else if (input.endsWith("h")) {
        val hours = input.substring(0, input.length - 1).toLong
        Duration.ofHours(hours)
      } else if (input.endsWith("d")) {
        val days = input.substring(0, input.length - 1).toLong
        Duration.ofDays(days)
      } else if (input.endsWith("w")) {
        val weeks = input.substring(0, input.length - 1).toLong
        Duration.ofDays(weeks * 7)
      } else if (input.endsWith("M")) {
        val months = input.substring(0, input.length - 1).toLong
        Duration.ofDays(months * 30)
      } else {
        throw new IllegalArgumentException("Invalid duration format")
      }
    }

    implicit def fromKlineToBaseBar(input: KlineEvent): BaseBar = {
      val endTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(input.klineData.endTime), ZoneOffset.UTC)
      new BaseBar(
        input.klineData.duration,
        endTime,
        input.klineData.openPrice,
        input.klineData.highPrice,
        input.klineData.lowPrice,
        input.klineData.closePrice,
        input.klineData.volume,
      )
    }

    def addBar(input: BaseBar, barSeries: BarSeries) = {
      ZIO
        .attempt(barSeries.addBar(input))
        .catchSome { case err: IllegalArgumentException =>
          ZIO.succeed(barSeries.addBar(input, true))
        }
        .catchAll(err => ZIO.logError(err.toString)) *> ZIO.log(s"Добавил новый бар: ${barSeries.getLastBar}")
    }
  }
}
