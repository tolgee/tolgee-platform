package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.publicConfiguration.PublicConfigurationAssembler
import io.tolgee.api.publicConfiguration.PublicConfigurationDTO
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/public/")
@Tag(name = "Public configuration controller")
class ConfigurationController(
  private val publicConfigurationAssembler: PublicConfigurationAssembler,
) : IController {
  @GetMapping(value = ["configuration"])
  @Operation(summary = "Get server configuration")
  fun getPublicConfiguration(): PublicConfigurationDTO {
    return publicConfigurationAssembler.toDto()
  }
}
