/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.machineTranslation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.machineTranslation.MachineTranslationProviderModel
import io.tolgee.service.machineTranslation.MtServiceConfigService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/public/machine-translation-providers"])
@Tag(name = "Machine Translation Providers")
class MachineTranslationProvidersController(
  private val mtServiceConfigService: MtServiceConfigService,
) {
  @GetMapping("")
  @Operation(
    description = "Get machine translation providers",
    summary = "Returns information about supported translation providers",
  )
  fun getInfo(): Map<String, MachineTranslationProviderModel> {
    return lazyInfo
  }

  val lazyInfo by lazy {
    mtServiceConfigService.services
      .mapNotNull {
        if (!it.value.second.isEnabled) {
          return@mapNotNull null
        }
        it.key.name to
          MachineTranslationProviderModel(
            it.value.second.supportedLanguages
              ?.toList(),
          )
      }.toMap()
  }
}
