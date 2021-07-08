package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.response.PublicConfigurationDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/public/")
@Tag(name="Public configuration controller")
class ConfigurationController @Autowired constructor(private val configuration: TolgeeProperties) : IController {

    @GetMapping(value = ["configuration"])
    @Operation(summary = "Returns server configuration information")
    fun getPublicConfiguration(): PublicConfigurationDTO {
        return PublicConfigurationDTO(configuration)
    }
}
