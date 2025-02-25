package io.tolgee.model.views

import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.Screenshot
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Tag
import io.tolgee.service.queryBuilders.Cursorable

data class KeyWithTranslationsView(
  val keyId: Long,
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
    fun of(
      queryData: Array<Any?>,
      languages: List<LanguageDto>,
    ): KeyWithTranslationsView {
      val data = mutableListOf(*queryData)
      val result =
        KeyWithTranslationsView(
          keyId = data.removeFirst() as Long,
          keyName = data.removeFirst() as String,
          keyIsPlural = data.removeFirst() as Boolean,
          keyPluralArgName = data.removeFirst() as String?,
          keyNamespaceId = data.removeFirst() as Long?,
          keyNamespace = data.removeFirst() as String?,
          keyDescription = data.removeFirst() as String?,
          screenshotCount = data.removeFirst() as Long,
          contextPresent = data.removeFirst() as Boolean,
        )

      (0 until data.size step 8).forEach { i ->
        val language = languages[i / 8].tag

        val id = data[i] as Long?
        if (id != null) {
          result.translations[language] =
            TranslationView(
              id = id,
              text = data[i + 1] as String?,
              state = (data[i + 2] ?: TranslationState.TRANSLATED) as TranslationState,
              outdated = data[i + 3] as Boolean,
              auto = data[i + 4] as Boolean,
              mtProvider = data[i + 5] as MtServiceType?,
              commentCount = (data[i + 6]) as Long,
              unresolvedCommentCount = (data[i + 7]) as Long,
            )
        }
      }
      return result
    }
  }

  override fun toCursorValue(property: String): String? {
    val path = property.split(".")
    return when (path[0]) {
      KeyWithTranslationsView::keyId.name -> keyId.toString()
      KeyWithTranslationsView::keyNamespace.name -> keyNamespace
      KeyWithTranslationsView::keyName.name -> keyName
      KeyWithTranslationsView::screenshotCount.name -> screenshotCount.toString()
      KeyWithTranslationsView::translations.name -> {
        val translation = translations[path[1]]
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
}
