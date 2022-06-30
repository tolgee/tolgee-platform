package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.controllers.IController
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/public/initial-data",
  ]
)
@Tag(name = "Initial data", description = "Returns initial always required by frontend")
class V2InitialDataController() : IController
