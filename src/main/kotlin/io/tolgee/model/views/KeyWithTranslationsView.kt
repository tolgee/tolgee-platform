package io.tolgee.model.views

import io.tolgee.model.enums.TranslationState

class KeyWithTranslationsView(queryData: Array<Any?>) {
    val keyId: Long
    val keyName: String
    val screenshotCount: Long
    val translations = mutableMapOf<String, TranslationView>()

    init {
        val data = mutableListOf(*queryData)
        keyId = data.removeFirst() as Long
        keyName = data.removeFirst() as String
        screenshotCount = data.removeFirst() as Long
        (0 until data.size step 4).forEach { i ->
            val language = data[i] as String?

            val id = data[i + 1] as Long?
            if (language != null && id != null) {
                translations[language] = TranslationView(
                        id = id,
                        text = data[i + 2] as String?,
                        state = (data[i + 3] ?: TranslationState.TRANSLATED) as TranslationState
                )
            }
        }
    }
}
