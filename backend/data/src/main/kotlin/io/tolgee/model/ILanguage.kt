package io.tolgee.model

interface ILanguage {
  var id: Long
  var tag: String
  var name: String
  var originalName: String?
  var flagEmoji: String?
  var aiTranslatorPromptDescription: String?
}
