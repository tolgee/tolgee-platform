package io.tolgee.development.testDataBuilder.data

class TranslationCopyTestData :
  BaseTestData(
    userName = "translation-copy-user",
    projectName = "Translation copy test",
  ) {
  val czechLanguage = projectBuilder.addCzech().self

  init {
    projectBuilder.apply {
      addKey {
        name = "greeting"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "Hello {name}, you have <b>new</b> messages"
        }
      }

      addKey {
        name = "unread-count"
        isPlural = true
        pluralArgName = "count"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "{count, plural, one {# unread message} other {# unread messages}}"
        }
      }
    }
  }
}
