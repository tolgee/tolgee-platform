package io.tolgee.service

import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.AutoTranslationConfig
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.AutoTranslationConfigRepository
import io.tolgee.service.machineTranslation.MtService
import org.springframework.stereotype.Service

@Service
class AutoTranslationService(
  private val translationService: TranslationService,
  private val autoTranslationConfigRepository: AutoTranslationConfigRepository,
  private val translationMemoryService: TranslationMemoryService,
  private val mtService: MtService
) {
  fun autoTranslate(key: Key) {
    val config = getAutoTranslationConfig(key.project)
    if (config.usingPrimaryMtService || config.usingTm) {
      autoTranslateAllExistingUntranslated(key)
      autoTranslateAllNotExisting(key)
    }
  }

  private fun getAutoTranslationConfig(project: Project) =
    autoTranslationConfigRepository.findOneByProject(project) ?: AutoTranslationConfig()

  private fun autoTranslateAllNotExisting(key: Key) {
    key.project.languages.filter { projectLanguage ->
      key.translations.find { it.language.id == projectLanguage.id } == null
    }.forEach { language ->
      val translation = Translation(key = key, language = language)
      translation.autoTranslate()
    }
  }

  private fun autoTranslateAllExistingUntranslated(key: Key) {
    getUntranslatedExistingTranslations(key).forEach {
      it.autoTranslate()
    }
  }

  private fun Translation.autoTranslate() {
    val translated = getAutoTranslatedText()
    if (translated != null) {
      this.text = translated
      this.state = TranslationState.MACHINE_TRANSLATED
      translationService.save(this)
    }
  }

  private fun Translation.getAutoTranslatedText(): String? {
    val config = getAutoTranslationConfig(this.key.project)
    if (config.usingTm) {
      translationMemoryService.getAutoTranslatedValue(this.key, this.language)
        ?.targetTranslationText?.let { return it }
    }
    if (config.usingPrimaryMtService) {
      try {
        return mtService.getPrimaryMachineTranslation(this.key, this.language)
      } catch (e: OutOfCreditsException) {
        return null
      }
    }
    return null
  }

  /**
   * Returns existing translations with null or emptry value
   */
  private fun getUntranslatedExistingTranslations(key: Key) = key.translations
    .filter { it.text.isNullOrEmpty() }
}
