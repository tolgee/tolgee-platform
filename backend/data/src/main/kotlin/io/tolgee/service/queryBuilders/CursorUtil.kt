package io.tolgee.service.queryBuilders

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.response.CursorValue
import org.springframework.data.domain.Sort
import java.util.Base64

class CursorUtil {
  companion object {
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

      val json = jacksonObjectMapper().writer().writeValueAsString(cursor)
      return Base64.getEncoder().encodeToString(json.toByteArray())
    }

    fun parseCursor(cursor: String): Map<String, CursorValue> {
      val json = Base64.getDecoder().decode(cursor)
      return jacksonObjectMapper().readValue(json)
    }
  }
}
