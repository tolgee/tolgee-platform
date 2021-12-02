package io.tolgee.service.machineTranslation

import io.tolgee.constants.MachineTranslationServiceType
import io.tolgee.constants.Message
import io.tolgee.dtos.request.SetMachineTranslationSettingsDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.MtServiceConfig
import io.tolgee.model.Project
import io.tolgee.repository.machineTranslation.MtServiceConfigRepository
import io.tolgee.service.LanguageService
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class MtServiceConfigService(
  private val applicationContext: ApplicationContext,
  private val mtServiceConfigRepository: MtServiceConfigRepository,
  private val languageService: LanguageService
) {
  /**
   * Returns services enabled for language id
   *
   * The primary service is first
   *
   * @return enabled translation services for project
   */
  fun getEnabledServices(languageId: Long): List<MachineTranslationServiceType> {
    // we have stored config, so lets return it.
    getStoredConfig(languageId)?.let { storedConfig ->
      val services = storedConfig.enabledServices.toList()
      // return just enabled services
      services.filter {
        this.services[it]?.second?.isEnabled ?: false
      }
        // primary first!
        .sortedByDescending { storedConfig.primaryService == it }
      return services
    }

    // stored config doesn't exist so lets use the default one
    // get services enabled by configuration
    // and put primary on the first place
    return services.asSequence()
      .sortedByDescending { it.value.first.defaultPrimary }
      .filter { it.value.first.defaultEnabled && it.value.second.isEnabled }.map { it.key }
      .toMutableList()
  }

  @Transactional
  fun setProjectSettings(project: Project, dto: SetMachineTranslationSettingsDto) {
    val storedConfigs = getStoredConfigs(project.id)
    val allLanguages = languageService.findAll(project.id)

    dto.settings.forEach { languageSetting ->
      val entity = storedConfigs.find { it.targetLanguage?.id == languageSetting.targetLanguageId }
        ?: MtServiceConfig().apply {
          this.project = project
          this.targetLanguage = allLanguages.find { languageSetting.targetLanguageId == it.id }
            ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
        }
      entity.apply {
        this.primaryService = languageSetting.primaryService
        this.enabledServices = languageSetting.enabledServices.filter {
          services[it]?.second?.isEnabled ?: false
        }.toSet()
      }
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
          enabledServices = services.filter { it.value.first.defaultEnabled && it.value.second.isEnabled }.keys
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

  private fun getStoredConfig(languageId: Long): MtServiceConfig? {
    val entities = mtServiceConfigRepository.findAllByTargetLanguageId(languageId)
    return entities.find { it.targetLanguage != null } ?: entities.find { it.targetLanguage == null }
  }

  private fun getStoredConfigs(projectId: Long): List<MtServiceConfig> {
    return mtServiceConfigRepository.findAllByProjectId(projectId)
  }


  val services by lazy {
    MachineTranslationServiceType.values().associateWith {
      (applicationContext.getBean(it.propertyClass) to applicationContext.getBean(it.providerClass))
    }
  }
}
