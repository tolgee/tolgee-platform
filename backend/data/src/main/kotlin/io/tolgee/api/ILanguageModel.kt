package io.tolgee.api

interface ILanguageModel {
  val id: Long
  val name: String
  val tag: String
  val originalName: String?
  val flagEmoji: String?
}
