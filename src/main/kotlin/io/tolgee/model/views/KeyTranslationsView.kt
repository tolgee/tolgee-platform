package io.tolgee.model.views

class KeyTranslationsView(queryData: Array<Any>) {
    val keyId: Long
    val keyName: String
    val translations = mutableMapOf<String, TranslationView>()

    init {
        val data = mutableListOf(*queryData)
        keyId = data.removeFirst() as Long
        keyName = data.removeFirst() as String

        (0 until data.size step 3).forEach { i ->
            val language = data[i] as String?

            val id = data[i + 1] as Long?
            if (language != null && id != null) {
                translations[language] = TranslationView(
                        id = id,
                        text = data[i + 2] as String?,
                )
            }
        }
    }
}
