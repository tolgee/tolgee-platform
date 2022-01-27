package io.tolgee.service

import io.tolgee.dtos.request.AutoTranslationSettingsDto
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
    val config = getConfig(key.project)
    if (config.usingPrimaryMtService || config.usingTm) {
      if (config.usingTm) {
        val untranslated = getUntranslatedTranslations(key)
        untranslated.forEach { translation ->
          val tmValue = translationMemoryService.getAutoTranslatedValue(key, translation.language)
          tmValue?.targetTranslationText
            .let { targetText -> if (targetText.isNullOrEmpty()) null else targetText }
            ?.let {
              translation.setValueAndState(it)
            }
        }
      }
      if (config.usingPrimaryMtService) {
        val untranslated = getUntranslatedTranslations(key)
        val languages = untranslated.map { it.language }
        mtService.getPrimaryMachineTranslation(key, languages)
          .zip(untranslated)
          .asSequence()
          .forEach { (translatedValue, translation) ->
            translatedValue?.let {
              translation.setValueAndState(it)
            }
          }
      }
    }
  }

  private fun Translation.setValueAndState(it: String) {
    this.state = TranslationState.MACHINE_TRANSLATED
    this.text = it
    this.key.translations.add(this)
    translationService.save(this)
  }

  private fun getUntranslatedTranslations(key: Key): List<Translation> {
    return getUntranslatedExistingTranslations(key) + createNonExistingTranslations(key)
  }

  private fun createNonExistingTranslations(key: Key): List<Translation> {
    return getLanguagesWithNoTranslation(key).map { language ->
      Translation(key = key, language = language)
    }
  }

  fun saveConfig(project: Project, dto: AutoTranslationSettingsDto) {
    val config = getConfig(project)
    config.usingTm = dto.usingTranslationMemory
    config.usingPrimaryMtService = dto.usingPrimaryMachineTranslationService
    saveConfig(config)
  }

  fun saveConfig(config: AutoTranslationConfig) {
    autoTranslationConfigRepository.save(config)
  }

  fun getConfig(project: Project) =
    autoTranslationConfigRepository.findOneByProject(project) ?: AutoTranslationConfig()

  private fun getLanguagesWithNoTranslation(key: Key) = key.project.languages
    .filter { projectLanguage ->
      key.translations.find { it.language.id == projectLanguage.id } == null
    }

  private fun Translation.getAutoTranslatedText(): String? {
    val config = getConfig(this.key.project)
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
   * Returns existing translations with null or empty value
   */
  private fun getUntranslatedExistingTranslations(key: Key) = key.translations
    .filter { it.text.isNullOrEmpty() }
}
