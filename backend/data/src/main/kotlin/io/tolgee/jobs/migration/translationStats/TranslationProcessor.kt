package io.tolgee.jobs.migration.translationStats

import io.tolgee.util.WordCounter
import org.springframework.batch.item.ItemProcessor

class TranslationProcessor : ItemProcessor<StatsMigrationTranslationView, TranslationStats> {
  override fun process(item: StatsMigrationTranslationView): TranslationStats {
    return TranslationStats(
      id = item.id, wordCount = item.text?.let { WordCounter.countWords(it, item.languageTag) } ?: 0,
      characterCount = item.text?.length ?: 0
    )
  }
}
