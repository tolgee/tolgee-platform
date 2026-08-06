package io.tolgee.development.testDataBuilder.data

/**
 * Test data for EeWordCountLimitListener: a single project with one key and
 * one English translation, whose word count can be controlled via [initialWordCount]
 * so tests can push the instance-wide word count over/under a limit by editing it.
 */
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
