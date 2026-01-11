package io.tolgee.model.views

import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.Screenshot
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Tag
import io.tolgee.service.queryBuilders.Cursorable
import java.sql.Timestamp

data class KeyWithTranslationsView(
  val keyId: Long,
  val createdAt: Timestamp,
  val keyName: String,
  val keyIsPlural: Boolean,
  val keyPluralArgName: String?,
  val keyNamespaceId: Long?,
  val keyNamespace: String?,
  val keyDescription: String?,
  val screenshotCount: Long,
  val contextPresent: Boolean,
  val translations: MutableMap<String, TranslationView> = mutableMapOf(),
) : Cursorable {
  lateinit var keyTags: List<Tag>
  var screenshots: Collection<Screenshot>? = null
  var tasks: List<KeyTaskView>? = null

  companion object {
    val FIELD_COUNT = 10

    fun of(
      queryData: Array<Any?>,
      languages: List<LanguageDto>,
    ): KeyWithTranslationsView {
      val data = mutableListOf(*queryData)
      val result =
        KeyWithTranslationsView(
          keyId = data.removeFirst() as Long,
          createdAt = data.removeFirst() as Timestamp,
          keyName = data.removeFirst() as String,
          keyIsPlural = data.removeFirst() as Boolean,
          keyPluralArgName = data.removeFirst() as String?,
          keyNamespaceId = data.removeFirst() as Long?,
          keyNamespace = data.removeFirst() as String?,
          keyDescription = data.removeFirst() as String?,
          screenshotCount = data.removeFirst() as Long,
          contextPresent = data.removeFirst() as Boolean,
        )

      (0 until data.size step FIELD_COUNT).forEach { i ->
        val language = languages[i / FIELD_COUNT].tag

        val id = data[i] as Long?
        result.translations[language] =
          TranslationView(
            id = id,
            text = data[i + 1] as String?,
            state =
              (
                data[i + 2]
                  ?: if (id == null) {
                    TranslationState.UNTRANSLATED
                  } else {
                    TranslationState.TRANSLATED
                  }
              ) as TranslationState,
            outdated = (data[i + 3] ?: false) as Boolean,
            auto = (data[i + 4] ?: false) as Boolean,
            mtProvider = data[i + 5] as MtServiceType?,
            commentCount = (data[i + 6] ?: 0L) as Long,
            unresolvedCommentCount = (data[i + 7] ?: 0L) as Long,
            activeSuggestionCount = (data[i + 8] ?: 0L) as Long,
            totalSuggestionCount = (data[i + 9] ?: 0L) as Long,
          )
      }

      return result
    }
  }

  override fun toCursorValue(property: String): String? {
    val path = property.split(".")
    return when (path[0]) {
      KeyWithTranslationsView::keyId.name -> keyId.toString()
      KeyWithTranslationsView::createdAt.name -> createdAt.time.toString()
      KeyWithTranslationsView::keyNamespace.name -> keyNamespace
      KeyWithTranslationsView::keyName.name -> keyName
      KeyWithTranslationsView::screenshotCount.name -> screenshotCount.toString()
      KeyWithTranslationsView::translations.name -> {
        val translation = translations[path[1]]
        when (path[2]) {
          TranslationView::text.name -> translation?.text
          TranslationView::id.name -> translation?.id?.toString()
          TranslationView::state.name -> translation?.state?.name
          else -> null
        }
      }

      else -> null
    }
  }
}
