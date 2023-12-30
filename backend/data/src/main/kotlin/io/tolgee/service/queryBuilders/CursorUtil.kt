package io.tolgee.service.queryBuilders

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.response.CursorValue
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import org.springframework.data.domain.Sort
import java.util.*

class CursorUtil {
  companion object {
    fun getCursor(
      item: KeyWithTranslationsView?,
      sort: Sort,
    ): String {
      val cursor =
        sort.map {
          it.property to
            CursorValue(
              direction = it.direction,
              value = getCursorValue(property = it.property, item),
            )
        }.toMap().toMutableMap()

      val json = jacksonObjectMapper().writer().writeValueAsString(cursor)
      return Base64.getEncoder().encodeToString(json.toByteArray())
    }

    private fun getCursorValue(
      property: String,
      item: KeyWithTranslationsView?,
    ): String? {
      val path = property.split(".")
      return when (path[0]) {
        KeyWithTranslationsView::keyId.name -> item?.keyId.toString()
        KeyWithTranslationsView::keyNamespace.name -> item?.keyNamespace
        KeyWithTranslationsView::keyName.name -> item?.keyName
        KeyWithTranslationsView::screenshotCount.name -> item?.screenshotCount.toString()
        KeyWithTranslationsView::translations.name -> {
          val translation = item?.translations?.get(path[1])
          when (path[2]) {
            TranslationView::text.name -> translation?.text
            TranslationView::id.name -> translation?.id.toString()
            TranslationView::state.name -> translation?.state?.name
            else -> null
          }
        }
        else -> null
      }
    }

    fun parseCursor(cursor: String): Map<String, CursorValue> {
      val json = Base64.getDecoder().decode(cursor)
      return jacksonObjectMapper().readValue(json)
    }
  }
}
