package io.tolgee.service.translation

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.AutoTranslationRequest
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.request.AutoTranslationSettingsDto
import io.tolgee.model.AutoTranslationConfig
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.AutoTranslationConfigRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.project.ProjectService
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import jakarta.persistence.EntityManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager

@Service
class AutoTranslationService(
  private val translationService: TranslationService,
  private val autoTranslationConfigRepository: AutoTranslationConfigRepository,
  private val translationMemoryService: TranslationMemoryService,
  private val mtService: MtService,
  private val languageService: LanguageService,
  private val batchJobService: BatchJobService,
  private val authenticationFacade: AuthenticationFacade,
  private val entityManager: EntityManager,
  private val transactionManager: PlatformTransactionManager,
  private val projectService: ProjectService,
) {
  val logger: Logger = LoggerFactory.getLogger(this::class.java)

  fun autoTranslateViaBatchJob(
    project: Project,
    request: AutoTranslationRequest,
    isHiddenJob: Boolean,
  ) {
    batchJobService.startJob(
      request,
      project,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.AUTO_TRANSLATE,
      isHiddenJob,
    )
  }

  fun autoTranslateViaBatchJob(
    projectId: Long,
    keyIds: List<Long>,
    baseLanguageId: Long,
    useTranslationMemory: Boolean? = null,
    useMachineTranslation: Boolean? = null,
    isBatch: Boolean,
    isHiddenJob: Boolean,
  ) {
    val project = entityManager.getReference(Project::class.java, projectId)
    val languageIds = languageService.findAll(projectId).map { it.id }.filter { it != baseLanguageId }
    val configs = this.getConfigs(project, languageIds)
    val request =
      AutoTranslationRequest().apply {
        target =
          languageIds.flatMap { languageId ->
            if (configs[languageId]?.usingTm == false && configs[languageId]?.usingPrimaryMtService == false) {
              return@flatMap listOf()
            }
            keyIds.map { keyId ->
              BatchTranslationTargetItem(
                keyId = keyId,
                languageId = languageId,
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
    languageId: Long,
  ) {
    val project = projectService.getDto(projectId)
    val config = getConfig(entityManager.getReference(Project::class.java, projectId), languageId)
    val translation =
      translationService.getTranslations(listOf(keyId), listOf(languageId)).singleOrNull() ?: Translation().apply {
        key = entityManager.getReference(Key::class.java, keyId)
        language = entityManager.getReference(Language::class.java, languageId)
      }

    val shouldTranslate =
      translation.state != TranslationState.DISABLED &&
        (
          translation.auto || translation.state == TranslationState.UNTRANSLATED || translation.text.isNullOrEmpty()
        )

    if (!shouldTranslate) {
      return
    }

    val (text, usedService, promptId) = getAutoTranslatedValue(config, translation) ?: return
    text ?: return

    translation.setValueAndState(project, text, usedService, promptId)
  }

  private fun getAutoTranslatedValue(
    config: AutoTranslationConfig,
    translation: Translation,
  ): Triple<String?, MtServiceType?, Long?>? {
    if (config.usingTm) {
      val value =
        translationMemoryService
          .getAutoTranslatedValue(
            translation.key,
            translation.language,
          )?.targetTranslationText

      if (!value.isNullOrBlank()) {
        return Triple(value, null, null)
      }
    }

    val targetLanguage = languageService.get(translation.language.id, config.project.id)

    if (config.usingPrimaryMtService) {
      val result =
        mtService
          .getMachineTranslations(config.project.id, true) {
            keyId = translation.key.id
            targetLanguageId = targetLanguage.id
            usePrimaryService = true
          }.singleOrNull()

      return result?.let { Triple(it.translatedText, it.service, it.promptId) }
    }

    return null
  }

  fun autoTranslateSyncWithRetry(
    key: Key,
    forcedLanguageTags: List<String>? = null,
    useTranslationMemory: Boolean? = null,
    useMachineTranslation: Boolean? = null,
    isBatch: Boolean,
  ) {
    tryUntilItDoesntBreakConstraint(maxRepeats = 10) {
      executeInNewTransaction(transactionManager) {
        autoTranslateSync(key, forcedLanguageTags, useTranslationMemory, useMachineTranslation, isBatch)
      }
    }
  }

  fun autoTranslateSync(
    key: Key,
    forcedLanguageTags: List<String>? = null,
    useTranslationMemory: Boolean? = null,
    useMachineTranslation: Boolean? = null,
    isBatch: Boolean,
  ) {
    val adjustedConfigs = getAdjustedConfigs(key, forcedLanguageTags, useTranslationMemory, useMachineTranslation)

    val translations =
      adjustedConfigs
        .map {
          if (it.override) {
            return@map it to
              translationService.getOrCreate(
                key = key,
                language = entityManager.getReference(Language::class.java, it.language.id),
              )
          }

          it to getUntranslatedTranslations(key, listOf(it.language)).firstOrNull()
        }.filter {
          it.second?.state != TranslationState.DISABLED
        }

    val toTmTranslate = translations.filter { it.first.usingTm }.mapNotNull { it.second }

    val translatedWithTm = autoTranslateUsingTm(toTmTranslate, key).filter { it.value }.keys

    val toMtTranslate =
      translations
        .filter { it.first.usingPrimaryMtService && !translatedWithTm.contains(it.second) }
        .mapNotNull { it.second }

    autoTranslateUsingMachineTranslation(toMtTranslate, key, isBatch)
  }

  /**
   * Returns config adjusted to reflect languages and translation methods overrides
   */
  private fun getAdjustedConfigs(
    key: Key,
    forcedLanguageTags: List<String>?,
    useTranslationMemory: Boolean?,
    useMachineTranslation: Boolean?,
  ) = getPerLanguageConfigs(key.project).mapNotNull {
    if (forcedLanguageTags != null) {
      // if we got languages provided, we want to override the existing language values
      it.override = true

      if (!forcedLanguageTags.contains(it.language.tag)) {
        return@mapNotNull null
      }
    }

    it.usingTm = useTranslationMemory ?: it.usingTm
    it.usingPrimaryMtService = (useMachineTranslation ?: it.usingPrimaryMtService)

    if (!it.usingTm && !it.usingPrimaryMtService) {
      return@mapNotNull null
    }

    it
  }

  fun getPerLanguageConfigs(project: Project): List<LanguageConfig> {
    val configs = getConfigs(project)
    val perLangConfig = configs.associateBy { it.targetLanguage?.id }

    val languages = languageService.findAll(project.id)
    return languages.map {
      val config = (perLangConfig[it.id] ?: perLangConfig[null] ?: AutoTranslationConfig())
      LanguageConfig(
        language = it,
        usingTm = config.usingTm,
        usingPrimaryMtService = config.usingPrimaryMtService,
      )
    }
  }

  data class LanguageConfig(
    val language: LanguageDto,
    var usingTm: Boolean,
    var usingPrimaryMtService: Boolean,
    var override: Boolean = false,
  )

  private fun autoTranslateUsingMachineTranslation(
    translations: List<Translation>,
    key: Key,
    isBatch: Boolean,
  ) {
    val project = projectService.getDto(key.project.id)
    val languages = translations.map { it.language.id }

    val result =
      mtService
        .getMachineTranslations(key.project.id, isBatch) {
          targetLanguageIds = languages
          keyId = key.id
          usePrimaryService = true
        }.associateBy { it.targetLanguageId }

    translations.forEach { translation ->
      result[translation.language.id]?.let {
        it.translatedText?.let { text ->
          translation.setValueAndState(project, text, it.service, it.promptId)
        }
      }
    }
  }

  /**
   * Returns map of translation and boolean value indicating if the translation was auto translated.
   */
  private fun autoTranslateUsingTm(
    toTranslate: List<Translation>,
    key: Key,
  ): Map<Translation, Boolean> {
    val project = projectService.getDto(key.project.id)
    return toTranslate.associateWith { translation ->
      val tmValue = translationMemoryService.getAutoTranslatedValue(key, translation.language)
      tmValue
        ?.targetTranslationText
        .let { targetText -> if (targetText.isNullOrEmpty()) null else targetText }
        ?.let {
          translation.setValueAndState(project, it, null)
        }
      (tmValue != null)
    }
  }

  private fun Translation.setValueAndState(
    project: ProjectDto,
    text: String,
    usedService: MtServiceType?,
    promptId: Long? = null,
  ) {
    this.resetFlags()
    if (project.translationProtection != TranslationProtection.PROTECT_REVIEWED) {
      this.state = TranslationState.TRANSLATED
    }
    this.auto = true
    this.text = text
    this.mtProvider = usedService
    this.promptId = promptId
    translationService.save(this)
  }

  private fun getUntranslatedTranslations(
    key: Key,
    languages: List<LanguageDto>?,
  ): List<Translation> {
    val languageIds = languages?.map { it.id }
    return getUntranslatedExistingTranslations(key, languageIds) + createNonExistingTranslations(key, languages)
  }

  private fun createNonExistingTranslations(
    key: Key,
    languages: List<LanguageDto>?,
  ): List<Translation> {
    return getLanguagesWithNoTranslation(key, languages).map { language ->
      val translation =
        Translation(
          key = key,
          language = entityManager.getReference(Language::class.java, language.id),
        )
      key.translations.add(translation)
      translation
    }
  }

  fun saveDefaultConfig(
    project: Project,
    dto: AutoTranslationSettingsDto,
  ): AutoTranslationConfig {
    val config = getDefaultConfig(project)
    config.usingTm = dto.usingTranslationMemory
    config.usingPrimaryMtService = dto.usingMachineTranslation
    config.enableForImport = dto.enableForImport
    return saveConfig(config)
  }

  fun deleteConfigsByProject(projectId: Long) {
    entityManager
      .createNativeQuery(
        "DELETE FROM auto_translation_config WHERE project_id = :projectId",
      ).setParameter("projectId", projectId)
      .executeUpdate()
  }

  fun saveConfig(config: AutoTranslationConfig): AutoTranslationConfig {
    return autoTranslationConfigRepository.save(config)
  }

  fun getConfigs(project: Project) =
    addDefaultConfig(project, autoTranslationConfigRepository.findAllByProject(project))

  private fun addDefaultConfig(
    project: Project,
    list: List<AutoTranslationConfig>?,
  ): List<AutoTranslationConfig> {
    val hasDefault =
      list?.any { autoTranslationConfig -> autoTranslationConfig.targetLanguage == null }
        ?: false
    if (!hasDefault) {
      return (list ?: listOf()) + listOf(AutoTranslationConfig().also { it.project = project })
    }
    return list!!
  }

  fun getConfigs(
    project: Project,
    targetLanguageIds: List<Long>,
  ): Map<Long, AutoTranslationConfig> {
    val configs = autoTranslationConfigRepository.findByProjectAndTargetLanguageIdIn(project, targetLanguageIds)
    val default =
      autoTranslationConfigRepository.findDefaultForProject(project)
        ?: autoTranslationConfigRepository.findDefaultForProject(project) ?: AutoTranslationConfig().also {
        it.project = project
      }

    return targetLanguageIds.associateWith { languageId ->
      (configs.find { it.targetLanguage?.id == languageId } ?: default)
    }
  }

  fun getConfig(
    project: Project,
    targetLanguageId: Long,
  ) = autoTranslationConfigRepository.findOneByProjectAndTargetLanguageId(project, targetLanguageId)
    ?: autoTranslationConfigRepository.findDefaultForProject(project) ?: AutoTranslationConfig().also {
    it.project = project
  }

  fun getDefaultConfig(project: Project) =
    autoTranslationConfigRepository.findOneByProjectAndTargetLanguageId(project, null) ?: AutoTranslationConfig()
      .also { it.project = project }

  private fun getLanguagesWithNoTranslation(
    key: Key,
    languages: List<LanguageDto>?,
  ): List<LanguageDto> {
    return (languages ?: languageService.getProjectLanguages(key.project.id))
      .filter { projectLanguage ->
        key.translations.find { it.language.id == projectLanguage.id } == null
      }
  }

  /**
   * Returns existing translations with null or empty value
   */
  private fun getUntranslatedExistingTranslations(
    key: Key,
    languageIds: List<Long>?,
  ) = key.translations
    .filter { languageIds?.contains(it.language.id) != false && it.text.isNullOrEmpty() }

  fun saveConfig(
    projectEntity: Project,
    dtos: List<AutoTranslationSettingsDto>,
  ): List<AutoTranslationConfig> {
    val configs = getConfigs(projectEntity).associateBy { it.targetLanguage?.id }

    languageService.findAll(projectEntity.id)
    val result =
      dtos.map { dto ->
        val targetLanguageReference =
          dto.languageId?.let { entityManager.getReference(Language::class.java, dto.languageId) }
        val config = (configs[dto.languageId] ?: AutoTranslationConfig())
        config.usingTm = dto.usingTranslationMemory
        config.usingPrimaryMtService = dto.usingMachineTranslation
        config.enableForImport = dto.enableForImport
        config.project = projectEntity
        config.targetLanguage = targetLanguageReference
        config
      }

    val toDelete =
      configs.values.filter { config ->
        result.find { it.targetLanguage?.id == config.targetLanguage?.id } == null
      }

    autoTranslationConfigRepository.deleteAll(toDelete)
    autoTranslationConfigRepository.saveAll(result)
    return addDefaultConfig(projectEntity, result)
  }
}
