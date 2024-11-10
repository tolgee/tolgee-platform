package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.PublicConfigurationDTO
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.MtServiceType
import io.tolgee.util.VersionProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/public/")
@Tag(name = "Public configuration controller")
class ConfigurationController
  @Autowired
  constructor(
    private val configuration: TolgeeProperties,
    private val applicationContext: ApplicationContext,
    private val publicBillingConfProvider: PublicBillingConfProvider,
    private val versionProvider: VersionProvider,
    private val cdFileStorageProvider: ContentDeliveryFileStorageProvider,
  ) : IController {
    @GetMapping(value = ["configuration"])
    @Operation(summary = "Get server configuration")
    fun getPublicConfiguration(): PublicConfigurationDTO {
      val machineTranslationServices =
        PublicConfigurationDTO.MtServicesDTO(
          defaultPrimaryService = getPrimaryMtService(),
          services = getMtServices(),
        )
      return PublicConfigurationDTO(
        properties = configuration,
        machineTranslationServices = machineTranslationServices,
        billing = publicBillingConfProvider(),
        versionProvider.version,
        contentDeliveryEnabled = cdFileStorageProvider.isServerContentDeliveryConfigured(),
      )
    }

    private fun getPrimaryMtService(): MtServiceType? {
      val primaryMtService =
        MtServiceType.values().find {
          applicationContext.getBean(it.propertyClass).defaultPrimary
        }
      return primaryMtService
    }

    private fun getMtServices(): Map<MtServiceType, PublicConfigurationDTO.MtServiceDTO> {
      val mtServices =
        MtServiceType.values()
          .sortedBy { it.order }
          .associateWith {
            PublicConfigurationDTO.MtServiceDTO(
              applicationContext.getBean(it.providerClass).isEnabled,
              applicationContext.getBean(it.propertyClass).defaultEnabled,
            )
          }
      return mtServices
    }
  }
