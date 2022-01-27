package io.tolgee.service

import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.request.AutoTranslationSettingsDto
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.AutoTranslationConfig
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.AutoTranslationConfigRepository
import io.tolgee.service.machineTranslation.MtService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AutoTranslationService(
  private val translationService: TranslationService,
  private val autoTranslationConfigRepository: AutoTranslationConfigRepository,
  private val translationMemoryService: TranslationMemoryService,
  private val mtService: MtService
) {
  val logger: Logger = LoggerFactory.getLogger(this::class.java)

  fun autoTranslate(key: Key) {
    val config = getConfig(key.project)
    if (config.usingPrimaryMtService || config.usingTm) {
      if (config.usingTm) {
        autoTranslateUsingTm(key)
      }
      if (config.usingPrimaryMtService) {
        autoTranslateUsingMachineTranslation(key)
      }
    }
  }

  private fun autoTranslateUsingMachineTranslation(key: Key) {
    val untranslated = getUntranslatedTranslations(key)
    val languages = untranslated.map { it.language }

    try {
      mtService.getPrimaryMachineTranslations(key, languages)
        .zip(untranslated)
        .asSequence()
        .forEach { (translateResult, translation) ->
          translateResult?.let {
            it.translatedText?.let { text ->
              translation.setValueAndState(text, it.usedService)
            }
          }
        }
    } catch (e: OutOfCreditsException) {
      logger.error(e.toString())
    }
  }

  private fun autoTranslateUsingTm(key: Key) {
    val untranslated = getUntranslatedTranslations(key)
    untranslated.forEach { translation ->
      val tmValue = translationMemoryService.getAutoTranslatedValue(key, translation.language)
      tmValue?.targetTranslationText
        .let { targetText -> if (targetText.isNullOrEmpty()) null else targetText }
        ?.let {
          translation.setValueAndState(it, null)
        }
    }
  }

  private fun Translation.setValueAndState(text: String, usedService: MtServiceType?) {
    this.state = TranslationState.TRANSLATED
    this.auto = true
    this.text = text
    this.mtProvider = usedService
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
    config.usingPrimaryMtService = dto.usingMachineTranslation
    saveConfig(config)
  }

  fun saveConfig(config: AutoTranslationConfig) {
    autoTranslationConfigRepository.save(config)
  }

  fun getConfig(project: Project) =
    autoTranslationConfigRepository.findOneByProject(project) ?: AutoTranslationConfig()
      .also { it.project = project }

  private fun getLanguagesWithNoTranslation(key: Key) = key.project.languages
    .filter { projectLanguage ->
      key.translations.find { it.language.id == projectLanguage.id } == null
    }

  /**
   * Returns existing translations with null or empty value
   */
  private fun getUntranslatedExistingTranslations(key: Key) = key.translations
    .filter { it.text.isNullOrEmpty() }
}
