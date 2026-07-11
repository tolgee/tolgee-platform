/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.PreferredOrganizationFacade
import io.tolgee.constants.Message
import io.tolgee.exceptions.PermissionException
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
  private val preferredOrganizationFacade: PreferredOrganizationFacade,
) {
  @GetMapping("")
  @Operation(
    summary = "Get preferred organization",
    description =
      "Returns preferred organization. " +
        "If server allows users to create organization, preferred organization is automatically created " +
        "if user doesn't have access to any organization.",
  )
  @OpenApiHideFromPublicDocs
  fun getPreferred(): PrivateOrganizationModel {
    return preferredOrganizationFacade.getPreferred() ?: throw PermissionException(Message.CANNOT_CREATE_ORGANIZATION)
  }
}
