/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.exceptions.PermissionException
import io.tolgee.facade.PrivateOrganizationModelFacade
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/preferred-organization"])
@Tag(name = "Preferred organization")
class PreferredOrganizationController(
  private val privateOrganizationModelFacade: PrivateOrganizationModelFacade,
) {
  @GetMapping("")
  @Operation(
    summary = "Get preferred organization",
    description =
      "Returns the preferred organization, or 403 when the user has none they can view.",
  )
  @OpenApiHideFromPublicDocs
  fun getPreferred(): PrivateOrganizationModel {
    return privateOrganizationModelFacade.getPreferred()
      ?: throw PermissionException(Message.CANNOT_CREATE_ORGANIZATION)
  }
}
