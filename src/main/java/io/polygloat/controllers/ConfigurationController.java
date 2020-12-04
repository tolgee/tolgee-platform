package io.polygloat.controllers;

import io.polygloat.configuration.polygloat.PolygloatProperties;
import io.polygloat.dtos.PublicConfigurationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/public/")
public class ConfigurationController implements IController {

    private PolygloatProperties configuration;

    @Autowired
    public ConfigurationController(PolygloatProperties configuration) {
        this.configuration = configuration;
    }

    @GetMapping(value = "configuration")
    public PublicConfigurationDTO getPublicConfiguration() {
        return new PublicConfigurationDTO(configuration);
    }
}
