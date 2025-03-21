package io.tolgee.service.key.resolvableImport

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportTranslationResolution
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportTranslationResolvableDto
import io.tolgee.model.Language
import io.tolgee.model.translation.Translation
import io.tolgee.service.key.resolvableImport.ResolvableImporter.TranslationToModify

class ResolvableTranslationImporter(
  private val keyContext: ResolvableKeyImporterContext,
  private val rootContext: ResolvableImportContext,
  private val language: LanguageDto,
  private val dto: ImportTranslationResolvableDto,
) {
  fun import() {
    val existingTranslation = rootContext.getExistingTranslation(keyContext.key, language.tag)

    val isEmpty = existingTranslation !== null && existingTranslation.text.isNullOrEmpty()

    val isNew = existingTranslation == null

    val translationExists = !isEmpty && !isNew

    if (validate(translationExists, dto, language.tag)) return

    if (language.base) {
      if (isNew || existingTranslation?.text != dto.text) {
        rootContext.outdatedKeys.add(keyContext.key.id)
      }
    }

    if (dto.resolution == ImportTranslationResolution.FORCE_OVERRIDE) {
      val updated =
        forceAddOrUpdateTranslation(
          language,
          dto.text,
          existingTranslation,
        )
      keyContext.translationsToModify.add(updated)
      return
    }

    if (isEmpty || (!isNew && dto.resolution == ImportTranslationResolution.OVERRIDE)) {
      keyContext.translationsToModify.add(TranslationToModify(existingTranslation!!, dto.text))
      return
    }

    if (isNew) {
      val translation =
        Translation(dto.text).apply {
          this.key = key
          this.language = rootContext.entityManager.getReference(Language::class.java, language.id)
        }
      keyContext.translationsToModify.add(TranslationToModify(translation, dto.text))
    }
  }

  private fun forceAddOrUpdateTranslation(
    language: LanguageDto,
    translationText: String,
    existingTranslation: Translation?,
  ): TranslationToModify {
    if (existingTranslation == null) {
      val translation =
        Translation(translationText).apply {
          this.key = key
          this.language = rootContext.entityManager.getReference(Language::class.java, language.id)
        }
      return TranslationToModify(translation, translationText)
    }

    return TranslationToModify(existingTranslation, translationText)
  }

  private fun validate(
    translationExists: Boolean,
    resolvable: ImportTranslationResolvableDto,
    languageTag: String,
  ): Boolean {
    if (resolvable.resolution == ImportTranslationResolution.FORCE_OVERRIDE) {
      return false
    }

    if (translationExists && resolvable.resolution == ImportTranslationResolution.NEW) {
      rootContext.errors.add(
        listOf(Message.TRANSLATION_EXISTS.code, keyContext.key.namespace?.name, keyContext.key.name, languageTag),
      )
      return true
    }

    if (!translationExists && resolvable.resolution != ImportTranslationResolution.NEW) {
      rootContext.errors.add(
        listOf(Message.TRANSLATION_NOT_FOUND.code, keyContext.key.namespace?.name, keyContext.key.name, languageTag),
      )
      return true
    }
    return false
  }
}
