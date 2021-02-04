package io.tolgee.controllers

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.PublicConfigurationDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/public/")
class ConfigurationController @Autowired constructor(private val configuration: TolgeeProperties) : IController {

    @GetMapping(value = ["configuration"])
    fun getPublicConfiguration(): PublicConfigurationDTO {
        return PublicConfigurationDTO(configuration)
    }
}