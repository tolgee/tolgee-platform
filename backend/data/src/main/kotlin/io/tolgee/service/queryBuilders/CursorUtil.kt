package io.tolgee.service.queryBuilders

import io.tolgee.dtos.response.CursorValue
import org.springframework.data.domain.Sort
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.Base64

class CursorUtil {
  companion object {
    private val objectMapper: ObjectMapper by lazy {
      jacksonObjectMapper()
    }

    fun getCursor(
      item: Cursorable?,
      sort: Sort,
    ): String {
      val cursor =
        sort
          .map {
            it.property to
              CursorValue(
                direction = it.direction,
                value = item?.toCursorValue(it.property),
              )
          }.toMap()
          .toMutableMap()

      val json = objectMapper.writer().writeValueAsString(cursor)
      return Base64.getEncoder().encodeToString(json.toByteArray())
    }

    fun parseCursor(cursor: String): Map<String, CursorValue> {
      val json = Base64.getDecoder().decode(cursor)
      return objectMapper.readValue(json)
    }
  }
}
