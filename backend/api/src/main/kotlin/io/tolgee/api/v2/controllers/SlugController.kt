/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.GenerateSlugDto
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/slug"])
@Tag(name = "Slug generation")
class SlugController(
  private val organizationService: OrganizationService,
  private val projectService: ProjectService,
) {
  @GetMapping("/validate-organization/{slug}")
  @Operation(summary = "Validate organization slug")
  fun validateOrganizationSlug(
    @PathVariable("slug") slug: String,
  ): Boolean {
    return organizationService.validateSlugUniqueness(slug)
  }

  @GetMapping("/validate-project/{slug}")
  @Operation(summary = "Validate project slug")
  @OpenApiHideFromPublicDocs
  fun validateProjectSlug(
    @PathVariable("slug") slug: String,
  ): Boolean {
    return projectService.validateSlugUniqueness(slug)
  }

  @PostMapping("/generate-organization", produces = [MediaType.APPLICATION_JSON_VALUE])
  @ReadOnlyOperation
  @Operation(summary = "Generate organization slug")
  fun generateOrganizationSlug(
    @RequestBody @Valid
    dto: GenerateSlugDto,
  ): String {
    return """"${organizationService.generateSlug(dto.name!!, dto.oldSlug)}""""
  }

  @PostMapping("/generate-project", produces = [MediaType.APPLICATION_JSON_VALUE])
  @ReadOnlyOperation
  @Operation(summary = "Generate project slug")
  @OpenApiHideFromPublicDocs
  fun generateProjectSlug(
    @RequestBody @Valid
    dto: GenerateSlugDto,
  ): String {
    return """"${projectService.generateSlug(dto.name!!, dto.oldSlug)}""""
  }
}
