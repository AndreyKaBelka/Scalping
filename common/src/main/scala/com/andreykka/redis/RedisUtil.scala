package com.andreykka.redis

import com.andreykka.dto.JsonData
import com.andreykka.json.JsonUtils
import zio.Task
import zio.json.ast.JsonCursor

object RedisUtil {

  def getDataForStream(data: String): Task[(String, String, JsonData)] = for {
    jsonData <- JsonUtils.parseJson(data)
  } yield (jsonData.eventType, jsonData.symbol, jsonData)

}
