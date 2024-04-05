package io.tolgee.api.v2.controllers.configurationProps

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.api.v2.controllers.IController
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/public/configuration-properties",
  ],
)
@OpenApiHideFromPublicDocs
class ConfigurationPropsController : IController {
  val docs get() = ConfigurationDocumentationProvider().docs

  @GetMapping(value = [""])
  @Operation(description = "Return server configuration properties documentation")
  fun get(): List<DocItem> {
    return docs
  }
}
