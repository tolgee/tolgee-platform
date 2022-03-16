package io.tolgee.jobs.migration.translationStats

import io.tolgee.model.translation.Translation
import io.tolgee.util.WordCounter
import org.springframework.batch.item.ItemProcessor

class TranslationProcessor : ItemProcessor<Translation, Translation> {
  override fun process(item: Translation): Translation {
    item.wordCount = item.text?.let { WordCounter.countWords(it, item.language.tag) }
    item.characterCount = item.text?.length ?: 0
    return item
  }
}
