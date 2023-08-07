package io.tolgee.service.translation

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.AutoTranslationRequest
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.request.AutoTranslationSettingsDto
import io.tolgee.model.AutoTranslationConfig
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.AutoTranslationConfigRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.LanguageService
import io.tolgee.service.machineTranslation.MtService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.persistence.EntityManager

@Service
class AutoTranslationService(
  private val translationService: TranslationService,
  private val autoTranslationConfigRepository: AutoTranslationConfigRepository,
  private val translationMemoryService: TranslationMemoryService,
  private val mtService: MtService,
  private val languageService: LanguageService,
  private val batchJobService: BatchJobService,
  private val authenticationFacade: AuthenticationFacade,
  private val entityManager: EntityManager
) {
  val logger: Logger = LoggerFactory.getLogger(this::class.java)

  fun autoTranslateViaBatchJob(project: Project, request: AutoTranslationRequest, isHiddenJob: Boolean) {
    batchJobService.startJob(
      request,
      project,
      authenticationFacade.userAccountEntity,
      BatchJobType.AUTO_TRANSLATE,
      isHiddenJob
    )
  }

  fun autoTranslateViaBatchJob(
    projectId: Long,
    keyIds: List<Long>,
    useTranslationMemory: Boolean? = null,
    useMachineTranslation: Boolean? = null,
    isBatch: Boolean,
    isHiddenJob: Boolean,
  ) {
    val project = entityManager.getReference(Project::class.java, projectId)
    val config = getConfig(project)
    val languageIds = languageService.findAll(projectId).map { it.id }
    val request = AutoTranslationRequest().apply {
      this.useTranslationMemory = useTranslationMemory ?: config.usingTm
      this.useMachineTranslation = useMachineTranslation ?: config.usingPrimaryMtService
      target = languageIds.flatMap { languageId ->
        keyIds.map { keyId ->
          BatchTranslationTargetItem(
            keyId = keyId,
            languageId = languageId
          )
        }
      }
    }
    autoTranslateViaBatchJob(project, request, isHiddenJob)
  }

  /**
   * It auto translates the strings, but only if it is not translated yet or if it has auto translated flag.
   */
  fun softAutoTranslate(
    projectId: Long,
    keyId: Long,
    languageId: Long
  ) {
    val config = getConfig(entityManager.getReference(Project::class.java, projectId))
    val translation =
      translationService.getTranslations(listOf(keyId), listOf(languageId)).singleOrNull() ?: Translation().apply {
        key = entityManager.getReference(Key::class.java, keyId)
        language = entityManager.getReference(Language::class.java, languageId)
      }

    val shouldTranslate =
      translation.auto || translation.state == TranslationState.UNTRANSLATED || translation.text.isNullOrEmpty()

    if (!shouldTranslate) {
      return
    }

    val (text, usedService) = getAutoTranslatedValue(config, translation) ?: return
    text ?: return

    translation.setValueAndState(text, usedService)
  }

  private fun getAutoTranslatedValue(
    config: AutoTranslationConfig,
    translation: Translation
  ): Pair<String?, MtServiceType?>? {

    if (config.usingTm) {
      val value = translationMemoryService.getAutoTranslatedValue(
        translation.key,
        translation.language
      )?.targetTranslationText

      if (!value.isNullOrBlank()) {
        return value to null
      }
    }

    if (config.usingPrimaryMtService) {
      val result = mtService.getPrimaryMachineTranslations(translation.key, listOf(translation.language), true)
        .singleOrNull()

      return result?.let { it.translatedText to it.usedService }
    }

    return null
  }

  fun autoTranslateSync(
    key: Key,
    languageTags: List<String>? = null,
    useTranslationMemory: Boolean? = null,
    useMachineTranslation: Boolean? = null,
    isBatch: Boolean,
  ) {
    val config = getConfig(key.project)

    if (useTranslationMemory ?: config.usingTm) {
      autoTranslateUsingTm(key, languageTags?.toSet())
    }
    if (useMachineTranslation ?: config.usingPrimaryMtService) {
      autoTranslateUsingMachineTranslation(key, languageTags?.toSet(), isBatch)
    }
  }

  private fun autoTranslateUsingMachineTranslation(key: Key, languageTags: Set<String>? = null, isBatch: Boolean) {
    val translations = languageTags?.let { getTranslations(key, languageTags) } ?: getUntranslatedTranslations(key)
    autoTranslateUsingMachineTranslation(translations, key, isBatch)
  }

  private fun autoTranslateUsingMachineTranslation(
    translations: List<Translation>,
    key: Key,
    isBatch: Boolean
  ) {
    val languages = translations.map { it.language }

    mtService.getPrimaryMachineTranslations(key, languages, isBatch)
      .zip(translations)
      .asSequence()
      .forEach { (translateResult, translation) ->
        translateResult?.let {
          it.translatedText?.let { text ->
            translation.setValueAndState(text, it.usedService)
          }
        }
      }
  }

  private fun autoTranslateUsingTm(key: Key, languageTags: Set<String>? = null) {
    val translations = languageTags?.let { getTranslations(key, languageTags) } ?: getUntranslatedTranslations(key)
    autoTranslateUsingTm(translations, key)
  }

  private fun autoTranslateUsingTm(
    untranslated: List<Translation>,
    key: Key
  ) {
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
    this.resetFlags()
    this.state = TranslationState.TRANSLATED
    this.auto = true
    this.text = text
    this.mtProvider = usedService
    translationService.save(this)
  }

  private fun getTranslations(key: Key, languageTags: Set<String>): List<Translation> {
    val languages = languageService.findByTags(languageTags, projectId = key.project.id)
    return languages.map {
      translationService.getOrCreate(key, it)
    }
  }

  private fun getUntranslatedTranslations(key: Key): List<Translation> {
    return getUntranslatedExistingTranslations(key) + createNonExistingTranslations(key)
  }

  private fun createNonExistingTranslations(key: Key): List<Translation> {
    return getLanguagesWithNoTranslation(key).map { language ->
      val translation = Translation(key = key, language = language)
      key.translations.add(translation)
      translation
    }
  }

  fun saveConfig(project: Project, dto: AutoTranslationSettingsDto) {
    val config = getConfig(project)
    config.usingTm = dto.usingTranslationMemory
    config.usingPrimaryMtService = dto.usingMachineTranslation
    config.enableForImport = dto.enableForImport
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
