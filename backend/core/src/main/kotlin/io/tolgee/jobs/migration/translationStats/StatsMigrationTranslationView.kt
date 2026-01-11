package io.tolgee.jobs.migration.translationStats

interface StatsMigrationTranslationView {
  val id: Long
  val text: String?
  val languageTag: String
}
