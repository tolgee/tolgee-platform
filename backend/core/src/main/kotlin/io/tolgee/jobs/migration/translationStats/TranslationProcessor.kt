package io.tolgee.jobs.migration.translationStats

import io.tolgee.util.TranslationStatsUtil
import org.springframework.batch.item.ItemProcessor

class TranslationProcessor : ItemProcessor<StatsMigrationTranslationView, TranslationStats> {
  override fun process(item: StatsMigrationTranslationView): TranslationStats {
    return TranslationStats(
      id = item.id,
      wordCount = TranslationStatsUtil.getWordCount(item.text, item.languageTag),
      characterCount = TranslationStatsUtil.getCharacterCount(item.text),
    )
  }
}
