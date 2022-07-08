package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.PublicConfigurationDTO
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.MtServiceType
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
class ConfigurationController @Autowired constructor(
  private val configuration: TolgeeProperties,
  private val applicationContext: ApplicationContext,
  private val publicBillingConfProvider: PublicBillingConfProvider
) : IController {

  @GetMapping(value = ["configuration"])
  @Operation(summary = "Returns server configuration information")
  fun getPublicConfiguration(): PublicConfigurationDTO {
    val machineTranslationServices = PublicConfigurationDTO.MtServicesDTO(
      defaultPrimaryService = getPrimaryMtService(),
      services = getMtServices()
    )
    return PublicConfigurationDTO(
      properties = configuration,
      machineTranslationServices = machineTranslationServices,
      billing = publicBillingConfProvider()
    )
  }

  private fun getPrimaryMtService(): MtServiceType? {
    val primaryMtService = MtServiceType.values().find {
      applicationContext.getBean(it.propertyClass).defaultPrimary
    }
    return primaryMtService
  }

  private fun getMtServices(): Map<MtServiceType, PublicConfigurationDTO.MtServiceDTO> {
    val mtServices = MtServiceType.values().associateWith {
      PublicConfigurationDTO.MtServiceDTO(
        applicationContext.getBean(it.providerClass).isEnabled,
        applicationContext.getBean(it.propertyClass).defaultEnabled
      )
    }
    return mtServices
  }
}
