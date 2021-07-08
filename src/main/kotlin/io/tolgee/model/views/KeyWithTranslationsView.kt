package io.tolgee.model.views

import io.tolgee.model.enums.TranslationState

data class KeyWithTranslationsView(
  val keyId: Long,
  val keyName: String,
  val screenshotCount: Long,
  val translations: MutableMap<String, TranslationView> = mutableMapOf(),
) {
  companion object {
    fun of(queryData: Array<Any?>): KeyWithTranslationsView {
      val data = mutableListOf(*queryData)
      val result = KeyWithTranslationsView(
        keyId = data.removeFirst() as Long,
        keyName = data.removeFirst() as String,
        screenshotCount = data.removeFirst() as Long
      )

      (0 until data.size step 4).forEach { i ->
        val language = data[i] as String?

        val id = data[i + 1] as Long?
        if (language != null && id != null) {
          result.translations[language] = TranslationView(
            id = id,
            text = data[i + 2] as String?,
            state = (data[i + 3] ?: TranslationState.TRANSLATED) as TranslationState
          )
        }
      }
      return result
    }
  }
}
