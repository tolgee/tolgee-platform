package io.tolgee.model.views

import io.tolgee.constants.MtServiceType
import io.tolgee.model.Screenshot
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Tag

data class KeyWithTranslationsView(
  val keyId: Long,
  val keyName: String,
  val screenshotCount: Long,
  val translations: MutableMap<String, TranslationView> = mutableMapOf(),
) {
  lateinit var keyTags: List<Tag>
  var screenshots: Collection<Screenshot>? = null

  companion object {
    fun of(queryData: Array<Any?>): KeyWithTranslationsView {
      val data = mutableListOf(*queryData)
      val result = KeyWithTranslationsView(
        keyId = data.removeFirst() as Long,
        keyName = data.removeFirst() as String,
        screenshotCount = data.removeFirst() as Long
      )

      (0 until data.size step 8).forEach { i ->
        val language = data[i] as String?

        val id = data[i + 1] as Long?
        if (language != null && id != null) {
          result.translations[language] = TranslationView(
            id = id,
            text = data[i + 2] as String?,
            state = (data[i + 3] ?: TranslationState.TRANSLATED) as TranslationState,
            auto = data[i + 4] as Boolean,
            mtProvider = data[i + 5] as MtServiceType?,
            commentCount = (data[i + 6]) as Long,
            unresolvedCommentCount = (data[i + 7]) as Long
          )
        }
      }
      return result
    }
  }
}
