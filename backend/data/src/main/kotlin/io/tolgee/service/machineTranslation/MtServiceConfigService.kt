package io.tolgee.service.machineTranslation

import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
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
import io.tolgee.service.LanguageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class MtServiceConfigService(
  private val applicationContext: ApplicationContext,
  private val mtServiceConfigRepository: MtServiceConfigRepository,
) {
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
  fun getEnabledServiceInfos(languageId: Long): List<MtServiceInfo> {
    return getEnabledServiceInfosByStoredConfig(languageId) ?: getEnabledServicesByDefaultServerConfig()
  }

  fun getPrimaryServices(languagesIds: List<Long>, project: Project): Map<Long, MtServiceInfo?> {
    val configs = getStoredConfigs(languagesIds, project)
    return languagesIds.associateWith { languageId ->
      configs[languageId]?.primaryServiceInfo
        ?: getDefaultPrimaryServiceInfo()
    }
  }

  private fun getDefaultPrimaryServiceInfo(): MtServiceInfo? {
    val defaultPrimaryService = getPrimaryServiceByDefaultConfig() ?: return null
    return MtServiceInfo(defaultPrimaryService, null)
  }

  private fun getEnabledServicesByDefaultServerConfig(): MutableList<MtServiceInfo> {
    return services.asSequence()
      .sortedByDescending { it.value.first.defaultPrimary }
      .filter { it.value.first.defaultEnabled && it.value.second.isEnabled }.map { it.key }
      .map { MtServiceInfo(it, null) }
      .toMutableList()
  }

  private fun getPrimaryServiceByDefaultConfig(): MtServiceType? {
    return services.filter { it.value.first.defaultPrimary }.keys.firstOrNull()
  }

  private fun getEnabledServiceInfosByStoredConfig(languageId: Long): List<MtServiceInfo>? {
    getStoredConfig(languageId)?.let { storedConfig ->
      return storedConfig.enabledServicesInfo.toList()
        // return just enabled services
        .filter {
          isServiceEnabledByServerConfig(it)
        }
        // primary first!
        .sortedByDescending { storedConfig.primaryService == it.serviceType }
    }
    return null
  }

  private fun isServiceEnabledByServerConfig(it: MtServiceInfo) =
    this.services[it.serviceType]?.second?.isEnabled ?: false

  private fun isServiceEnabledByServerConfig(it: MtServiceType) =
    this.services[it]?.second?.isEnabled ?: false

  @Transactional
  fun setProjectSettings(project: Project, dto: SetMachineTranslationSettingsDto) {
    val storedConfigs = getStoredConfigs(project.id)
    val allLanguages = languageService.findAll(project.id).associateBy { it.id }

    validateSettings(dto.settings, allLanguages)

    dto.settings.forEach { languageSetting ->
      val entity = storedConfigs.find { it.targetLanguage?.id == languageSetting.targetLanguageId }
        ?: MtServiceConfig().apply {
          this.project = project
          this.targetLanguage = languageSetting.targetLanguageId
            ?.let { allLanguages.getLanguageOrThrow(it) }
        }

      entity.primaryService = languageSetting.primaryService
      entity.enabledServices = getEnabledServices(languageSetting)
      setFormalities(entity, languageSetting)
      save(entity)
    }

    val toDelete = storedConfigs.filter { storedConfig ->
      dto.settings.find { it.targetLanguageId == storedConfig.targetLanguage?.id } == null
    }

    delete(toDelete)
  }

  private fun Map<Long, Language>.getLanguageOrThrow(id: Long?): Language? {
    id ?: return null
    return this[id] ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
  }

  private fun validateSettings(settings: List<MachineTranslationLanguagePropsDto>, allLanguages: Map<Long, Language>) {
    settings.forEach {
      validateSetting(allLanguages, it)
    }
  }

  private fun validateSetting(
    allLanguages: Map<Long, Language>,
    languageProps: MachineTranslationLanguagePropsDto
  ) {
    val language = allLanguages.getLanguageOrThrow(languageProps.targetLanguageId) ?: return
    validateLanguageSupported(languageProps, language)
    validateFormality(languageProps, language)
  }

  private fun validateFormality(
    languageProps: MachineTranslationLanguagePropsDto,
    language: Language,
  ) {
    languageProps.enabledServicesInfo.forEach {
      if (it.formality == Formality.DEFAULT || it.formality == null) {
        return@forEach
      }
      val isFormalitySupported =
        this.services[it.serviceType]?.second?.isLanguageFormalitySupported(language.tag) ?: false
      if (!isFormalitySupported) {
        throw BadRequestException(
          Message.FORMALITY_NOT_SUPPORTED_BY_SERVICE,
          listOf(it.serviceType.name, language.tag, it.formality)
        )
      }
    }
  }

  private fun validateLanguageSupported(
    it: MachineTranslationLanguagePropsDto,
    language: Language
  ) {
    val allServices = it.enabledServicesInfo.map { it.serviceType } + it.enabledServices
    allServices.forEach {
      val isLanguageSupported = this.services[it]?.second?.isLanguageSupported(language.tag) ?: false
      if (!isLanguageSupported) {
        throw BadRequestException(Message.LANGUAGE_NOT_SUPPORTED_BY_SERVICE, listOf(it.name, language.tag))
      }
    }
  }

  private fun getEnabledServices(languageSetting: MachineTranslationLanguagePropsDto): MutableSet<MtServiceType> {
    val allServiceTypes = languageSetting.enabledServices + languageSetting.enabledServicesInfo.map { it.serviceType }
    return allServiceTypes.filter { isServiceEnabledByServerConfig(it) }.toMutableSet()
  }

  private fun setFormalities(entity: MtServiceConfig, languageSetting: MachineTranslationLanguagePropsDto) {
    languageSetting.enabledServicesInfo.forEach {
      when (it.serviceType) {
        MtServiceType.AWS -> entity.awsFormality = it.formality ?: Formality.DEFAULT
        MtServiceType.DEEPL -> entity.deeplFormality = it.formality ?: Formality.DEFAULT
        MtServiceType.TOLGEE -> entity.tolgeeFormality = it.formality ?: Formality.DEFAULT
        else -> {
          if (it.formality == null) {
            return
          }
          throw BadRequestException(Message.FORMALITY_NOT_SUPPORTED_BY_SERVICE)
        }
      }
    }
  }

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
          config.enabledServices = config.enabledServices
            .sortedByDescending { config.primaryService == it }
            .toSortedSet()
        }
      }
  }

  private fun getDefaultConfig(project: Project): MtServiceConfig {
    return MtServiceConfig().apply {
      enabledServices = services.filter { it.value.first.defaultEnabled && it.value.second.isEnabled }
        .keys.toMutableSet()
      this.project = project
      primaryService = services.entries.find { it.value.first.defaultPrimary }?.key
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

  fun deleteAllByTargetLanguageId(projectId: Long) {
    this.mtServiceConfigRepository.deleteAllByTargetLanguageId(projectId)
  }

  private fun getStoredConfig(languageId: Long): MtServiceConfig? {
    val entities = mtServiceConfigRepository.findAllByTargetLanguageId(languageId)
    return entities.find { it.targetLanguage != null } ?: entities.find { it.targetLanguage == null }
  }

  private fun getStoredConfigs(languageIds: List<Long>, project: Project): Map<Long, MtServiceConfig?> {
    val entities = mtServiceConfigRepository.findAllByTargetLanguageIdIn(languageIds, project)
    return languageIds.associateWith { languageId ->
      entities.find { it.targetLanguage?.id == languageId } ?: entities.find { it.targetLanguage == null }
    }
  }

  private fun getStoredConfigs(projectId: Long): List<MtServiceConfig> {
    return mtServiceConfigRepository.findAllByProjectId(projectId)
  }

  fun getLanguageInfo(project: ProjectDto): List<MtLanguageInfo> {
    return languageService.findAll(project.id).map { language ->
      val supportedServices =
        services.filter { it.value.second.isLanguageSupported(language.tag) && it.value.second.isEnabled }.map {
          MtSupportedService(it.key, it.value.second.isLanguageFormalitySupported(language.tag))
        }
      MtLanguageInfo(language = language, supportedServices)
    }
  }

  val services by lazy {
    MtServiceType.values().associateWith {
      (applicationContext.getBean(it.propertyClass) to applicationContext.getBean(it.providerClass))
    }
  }
}
