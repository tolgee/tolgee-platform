package io.tolgee.development.testDataBuilder.data

class WordCountLimitTestData(
  initialWordCount: Int = 0,
) : BaseTestData("word-count-limit-user", "Word Count Limit Project") {
  init {
    projectBuilder.apply {
      addKey { name = "wcl-key1" }.build {
        addTranslation("en", wordsText(initialWordCount))
      }
    }
  }

  companion object {
    fun wordsText(count: Int): String = (1..count).joinToString(" ") { "w$it" }
  }
}
