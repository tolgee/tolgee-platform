package io.tolgee.configuration.batch

import io.tolgee.model.translation.Translation
import io.tolgee.service.TranslationService
import org.springframework.batch.item.ItemReader

class FindAndReplaceTranslationReader(
  val translationService: TranslationService
): ItemReader<Translation> {



  override fun read(): Translation? {
    TODO("Not yet implemented")
  }

}
