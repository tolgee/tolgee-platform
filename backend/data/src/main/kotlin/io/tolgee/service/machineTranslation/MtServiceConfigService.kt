package io.tolgee.service.machineTranslation

import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.request.SetMachineTranslationSettingsDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.MtServiceConfig
import io.tolgee.model.Project
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
  fun getEnabledServices(languageId: Long): List<MtServiceType> {
    getEnabledServicesByStoredConfig(languageId)?.let {
      return it
    }
    return getEnabledServicesByDefaultConfig()
  }

  fun getPrimaryServices(languagesIds: List<Long>): Map<Long, MtServiceType?> {
    val configs = getStoredConfigs(languagesIds)
    return languagesIds.associateWith { languageId ->
      configs.find { config ->
        config?.targetLanguage?.id == languageId
      }?.let { return@associateWith it.primaryService } ?: getPrimaryServiceByDefaultConfig()
    }
  }

  private fun getEnabledServicesByDefaultConfig(): MutableList<MtServiceType> {
    return services.asSequence()
      .sortedByDescending { it.value.first.defaultPrimary }
      .filter { it.value.first.defaultEnabled && it.value.second.isEnabled }.map { it.key }
      .toMutableList()
  }

  private fun getPrimaryServiceByDefaultConfig(): MtServiceType? {
    return services.filter { it.value.first.defaultPrimary }.keys.firstOrNull()
  }

  private fun getEnabledServicesByStoredConfig(languageId: Long): List<MtServiceType>? {
    getStoredConfig(languageId)?.let { storedConfig ->
      return storedConfig.enabledServices.toList()
        // return just enabled services
        .filter {
          this.services[it]?.second?.isEnabled ?: false
        }
        // primary first!
        .sortedByDescending { storedConfig.primaryService == it }
    }
    return null
  }

  @Transactional
  fun setProjectSettings(project: Project, dto: SetMachineTranslationSettingsDto) {
    val storedConfigs = getStoredConfigs(project.id)
    val allLanguages = languageService.findAll(project.id)

    dto.settings.forEach { languageSetting ->
      val entity = storedConfigs.find { it.targetLanguage?.id == languageSetting.targetLanguageId }
        ?: MtServiceConfig().apply {
          this.project = project
          this.targetLanguage = languageSetting.targetLanguageId
            ?.let {
              allLanguages.find { languageSetting.targetLanguageId == it.id }
                ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
            }
        }

      entity.primaryService = languageSetting.primaryService
      entity.enabledServices = languageSetting.enabledServices.filter {
        services[it]?.second?.isEnabled ?: false
      }.toMutableSet()
      save(entity)
    }

    val toDelete = storedConfigs.filter { storedConfig ->
      dto.settings.find { it.targetLanguageId == storedConfig.targetLanguage?.id } == null
    }

    delete(toDelete)
  }

  fun getProjectSettings(project: Project): List<MtServiceConfig> {
    return getStoredConfigs(project.id).sortedBy { it.targetLanguage == null }.toMutableList().also { configs ->
      if (configs.find { it.targetLanguage == null } == null) {
        val defaultConfig = MtServiceConfig().apply {
          enabledServices = services.filter { it.value.first.defaultEnabled && it.value.second.isEnabled }
            .keys.toMutableSet()
          this.project = project
          primaryService = services.entries.find { it.value.first.defaultPrimary }?.key
        }
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

  private fun getStoredConfigs(languageIds: List<Long>): List<MtServiceConfig?> {
    val entities = mtServiceConfigRepository.findAllByTargetLanguageIdIn(languageIds)
    return languageIds.map { languageId ->
      entities.find { it.targetLanguage?.id == languageId } ?: entities.find { it.targetLanguage == null }
    }
  }

  private fun getStoredConfigs(projectId: Long): List<MtServiceConfig> {
    return mtServiceConfigRepository.findAllByProjectId(projectId)
  }

  val services by lazy {
    MtServiceType.values().associateWith {
      (applicationContext.getBean(it.propertyClass) to applicationContext.getBean(it.providerClass))
    }
  }
}
