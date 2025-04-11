package io.tolgee.service.machineTranslation

import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.request.MachineTranslationLanguagePropsDto
import io.tolgee.dtos.request.SetMachineTranslationSettingsDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.mtServiceConfig.Formality
import io.tolgee.model.mtServiceConfig.MtServiceConfig
import io.tolgee.repository.machineTranslation.MtServiceConfigRepository
import io.tolgee.service.PromptService
import io.tolgee.service.language.LanguageService
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MtServiceConfigService(
  private val applicationContext: ApplicationContext,
  private val mtServiceConfigRepository: MtServiceConfigRepository,
  private val entityManager: EntityManager,
) {
  @Autowired
  private lateinit var promptService: PromptService

  @set:Autowired
  @set:Lazy
  lateinit var languageService: LanguageService

  /**
   * Returns services enabled for language id
   *
   * The primary service is first
   *
   * @return enabled translation services for project
   */
  fun getEnabledServiceInfos(language: LanguageDto): List<MtServiceInfo> {
    return getEnabledServiceInfosByStoredConfig(language)
      ?: getEnabledServicesByDefaultServerConfig(language)
  }

  fun getPrimaryServices(
    languagesIds: List<Long>,
    projectId: Long,
  ): Map<Long, MtServiceInfo?> {
    val configs = getStoredConfigs(languagesIds, projectId)
    return languagesIds.associateWith { languageId ->
      configs[languageId]?.primaryServiceInfo
        ?: getDefaultPrimaryServiceInfo()
    }
  }

  private fun getDefaultPrimaryServiceInfo(): MtServiceInfo? {
    val defaultPrimaryService = getPrimaryServiceByDefaultConfig() ?: return null
    return MtServiceInfo(defaultPrimaryService, null)
  }

  private fun getEnabledServicesByDefaultServerConfig(language: LanguageDto): MutableList<MtServiceInfo> {
    return services.asSequence()
      .sortedBy { it.key.order }
      .sortedByDescending { it.value.first?.defaultPrimary ?: true }
      .filter { it.value.first?.defaultEnabled ?: true && it.value.second.isEnabled && language.isSupportedBy(it.key) }
      .map { it.key }
      .map { MtServiceInfo(it, null) }
      .toMutableList()
  }

  private fun getPrimaryServiceByDefaultConfig(): MtServiceType? {
    return services.filter {
      (it.value.first?.defaultPrimary ?: true) && it.value.second.isEnabled
    }.keys.minByOrNull { it.order }
  }

  private fun getEnabledServiceInfosByStoredConfig(language: LanguageDto): List<MtServiceInfo>? {
    getStoredConfig(language.id)?.let { storedConfig ->
      return storedConfig.enabledServicesInfo.toList()
        // return just enabled services
        .filter {
          isServiceEnabledByServerConfig(it) && language.isSupportedBy(it.serviceType)
        }.sortedBy { it.serviceType.order }
        // primary first!
        .sortedByDescending { storedConfig.primaryService == it.serviceType }
    }
    return null
  }

  private fun LanguageDto.isSupportedBy(serviceType: MtServiceType): Boolean {
    return services[serviceType]?.second?.isLanguageSupported(this.tag) ?: return false
  }

  private fun isServiceEnabledByServerConfig(it: MtServiceInfo) = getServiceProcessor(it)?.isEnabled ?: false

  private fun isServiceEnabledByServerConfig(it: MtServiceType) = this.services[it]?.second?.isEnabled ?: false

  @Transactional
  fun setProjectSettings(
    project: Project,
    dto: SetMachineTranslationSettingsDto,
  ) {
    val storedConfigs = getStoredConfigs(project.id)
    val allLanguages = languageService.findAll(project.id).associateBy { it.id }

    validateSettings(dto.settings, allLanguages)

    dto.settings.forEach { languageSetting ->
      val entity =
        storedConfigs.find { it.targetLanguage?.id == languageSetting.targetLanguageId }
          ?: MtServiceConfig().apply {
            this.project = project
            languageSetting.targetLanguageId?.let {
              this.targetLanguage = entityManager.getReference(Language::class.java, it)
            }
          }

      entity.prompt =
        (
          languageSetting.primaryServiceInfo?.promptId
            ?: languageSetting.enabledServicesInfo?.find { it.promptId != null }?.promptId
        )?.let {
          promptService.findPrompt(
            project.id,
            it,
          )
        }

      setPrimaryService(entity, languageSetting)
      entity.enabledServices = getEnabledServices(languageSetting)
      setFormalities(entity, languageSetting)
      save(entity)
      entityManager.flush()
    }

    val toDelete =
      storedConfigs.filter { storedConfig ->
        dto.settings.find { it.targetLanguageId == storedConfig.targetLanguage?.id } == null
      }

    delete(toDelete)
  }

  private fun setPrimaryService(
    entity: MtServiceConfig,
    languageSetting: MachineTranslationLanguagePropsDto,
  ) {
    // this setting is already deprecated (it doesn't support formality), but we need to support it for now
    entity.primaryService = languageSetting.primaryService

    // this is the new approach
    val primaryServiceInfo = languageSetting.primaryServiceInfo ?: return

    entity.primaryService = primaryServiceInfo.serviceType
  }

  private fun Map<Long, LanguageDto>.getLanguageOrThrow(id: Long?): LanguageDto? {
    id ?: return null
    return this[id] ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
  }

  private fun validateSettings(
    settings: List<MachineTranslationLanguagePropsDto>,
    allLanguages: Map<Long, LanguageDto>,
  ) {
    settings.forEach {
      validateSetting(allLanguages, it)
    }
  }

  private fun validateSetting(
    allLanguages: Map<Long, LanguageDto>,
    languageProps: MachineTranslationLanguagePropsDto,
  ) {
    val language = allLanguages.getLanguageOrThrow(languageProps.targetLanguageId) ?: return
    validateLanguageSupported(languageProps, language)
    validateFormality(languageProps, language)
  }

  private fun validateFormality(
    languageProps: MachineTranslationLanguagePropsDto,
    language: LanguageDto,
  ) {
    validateEnabledServicesFormality(languageProps, language)
    validatePrimaryServiceFormality(languageProps, language)
  }

  private fun validatePrimaryServiceFormality(
    languageProps: MachineTranslationLanguagePropsDto,
    language: LanguageDto,
  ) {
    val primaryServiceInfo = languageProps.primaryServiceInfo ?: return
    if (primaryServiceInfo.formality === null) {
      return
    }
    val isSupported =
      getServiceProcessor(primaryServiceInfo)?.isLanguageFormalitySupported(language.tag)
        ?: false

    if (!isSupported) {
      throwFormalityNotSupported(primaryServiceInfo, language)
    }
  }

  private fun validateEnabledServicesFormality(
    languageProps: MachineTranslationLanguagePropsDto,
    language: LanguageDto,
  ) {
    languageProps.enabledServicesInfo?.forEach {
      if (it.formality == Formality.DEFAULT || it.formality == null) {
        return@forEach
      }
      val isFormalitySupported =
        getServiceProcessor(it)?.isLanguageFormalitySupported(language.tag) ?: false
      if (!isFormalitySupported) {
        throwFormalityNotSupported(it, language)
      }
    }
  }

  private fun throwFormalityNotSupported(
    mtServiceInfo: MtServiceInfo,
    language: LanguageDto,
  ) {
    throw BadRequestException(
      Message.FORMALITY_NOT_SUPPORTED_BY_SERVICE,
      listOf(mtServiceInfo.serviceType.name, language.tag, mtServiceInfo.formality!!),
    )
  }

  private fun getServiceProcessor(it: MtServiceInfo) = this.services[it.serviceType]?.second

  private fun validateLanguageSupported(
    it: MachineTranslationLanguagePropsDto,
    language: LanguageDto,
  ) {
    val allServices = getAllEnabledOrPrimaryServices(it)
    allServices.forEach {
      val isLanguageSupported = this.services[it]?.second?.isLanguageSupported(language.tag) ?: false
      if (!isLanguageSupported) {
        throw BadRequestException(Message.LANGUAGE_NOT_SUPPORTED_BY_SERVICE, listOf(it.name, language.tag))
      }
    }
  }

  private fun getAllEnabledOrPrimaryServices(it: MachineTranslationLanguagePropsDto) =
    it.enabledServicesInfoNotNull.map { it.serviceType } +
      it.enabledServicesNotNull +
      listOfNotNull(it.primaryService) +
      listOfNotNull(it.primaryServiceInfo?.serviceType)

  private fun getEnabledServices(languageSetting: MachineTranslationLanguagePropsDto): MutableSet<MtServiceType> {
    val allServiceTypes =
      languageSetting.enabledServicesNotNull + languageSetting.enabledServicesInfoNotNull.map { it.serviceType }
    return allServiceTypes.filter { isServiceEnabledByServerConfig(it) }.toMutableSet()
  }

  private fun setFormalities(
    entity: MtServiceConfig,
    languageSetting: MachineTranslationLanguagePropsDto,
  ) {
    val services = languageSetting.enabledServicesInfoNotNull.toMutableList()
    languageSetting.primaryServiceInfo?.let {
      services.add(it)
    }
    services.forEach {
      when (it.serviceType) {
        MtServiceType.AWS -> entity.awsFormality = it.formality ?: Formality.DEFAULT
        MtServiceType.DEEPL -> entity.deeplFormality = it.formality ?: Formality.DEFAULT
        MtServiceType.PROMPT -> entity.promptFormality = it.formality ?: Formality.DEFAULT
        else -> {}
      }
    }
  }

  @Transactional
  fun getProjectSettings(project: Project): List<MtServiceConfig> {
    return getStoredConfigs(project.id)
      .sortedBy { it.targetLanguage?.tag }
      .sortedBy { it.targetLanguage != null }
      .toMutableList()
      .also { configs ->
        val thereIsNoDefaultConfig = configs.find { it.targetLanguage == null } == null
        if (thereIsNoDefaultConfig) {
          val defaultConfig = getDefaultConfig(project)
          configs.add(0, defaultConfig)
        }
        configs.forEach { config ->
          // put primary service first
          config.enabledServices =
            config.enabledServices
              .sortedByDescending { config.primaryService == it }
              .toSortedSet()
        }
      }
  }

  private fun getDefaultConfig(project: Project): MtServiceConfig {
    return MtServiceConfig().apply {
      enabledServices =
        services.filter { it.value.first?.defaultEnabled ?: true && it.value.second.isEnabled }
          .keys.toMutableSet()
      this.project = project
      primaryService = getPrimaryServiceByDefaultConfig()
    }
  }

  private fun save(entity: MtServiceConfig) {
    this.mtServiceConfigRepository.save(entity)
  }

  fun saveAll(entities: List<MtServiceConfig>) {
    this.mtServiceConfigRepository.saveAll(entities)
  }

  private fun delete(entities: List<MtServiceConfig>) {
    this.mtServiceConfigRepository.deleteAll(entities)
  }

  fun deleteAllByProjectId(projectId: Long) {
    this.mtServiceConfigRepository.deleteAllByProjectId(projectId)
  }

  private fun getStoredConfig(languageId: Long): MtServiceConfig? {
    val entities = mtServiceConfigRepository.findAllByTargetLanguageId(languageId)
    return entities.find { it.targetLanguage != null } ?: entities.find { it.targetLanguage == null }
  }

  private fun getStoredConfigs(
    languageIds: List<Long>,
    projectId: Long,
  ): Map<Long, MtServiceConfig?> {
    val entities = mtServiceConfigRepository.findAllByTargetLanguageIdIn(languageIds, projectId)
    return languageIds.associateWith { languageId ->
      entities.find { it.targetLanguage?.id == languageId } ?: entities.find { it.targetLanguage == null }
    }
  }

  private fun getStoredConfigs(projectId: Long): List<MtServiceConfig> {
    return mtServiceConfigRepository.findAllByProjectId(projectId)
  }

  fun getLanguageInfo(project: ProjectDto): List<MtLanguageInfo> {
    val result: MutableList<MtLanguageInfo> = mutableListOf()
    result.add(
      MtLanguageInfo(
        language = null,
        supportedServices =
          services.filter {
            it.value.second.isEnabled
          }.map {
            MtSupportedService(it.key, it.value.second.formalitySupportingLanguages !== null)
          },
      ),
    )
    languageService.findAll(project.id).forEach { language ->
      val supportedServices =
        services.filter { it.value.second.isLanguageSupported(language.tag) && it.value.second.isEnabled }.map {
          MtSupportedService(it.key, it.value.second.isLanguageFormalitySupported(language.tag))
        }
      result.add(MtLanguageInfo(language = language, supportedServices))
    }
    return result
  }

  val services by lazy {
    MtServiceType.entries.associateWith {
      (it.propertyClass?.let { applicationContext.getBean(it) } to applicationContext.getBean(it.providerClass))
    }
  }
}
