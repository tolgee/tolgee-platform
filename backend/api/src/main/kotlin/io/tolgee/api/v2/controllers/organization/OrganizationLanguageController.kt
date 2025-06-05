package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.cacheable.OrganizationLanguageDto
import io.tolgee.hateoas.language.OrganizationLanguageModel
import io.tolgee.hateoas.language.OrganizationLanguageModelAssembler
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.language.LanguageService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}"])
@Tag(name = "Organizations")
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class OrganizationLanguageController(
  private val languageService: LanguageService,
  private val organizationLanguageModelAssembler: OrganizationLanguageModelAssembler,
  private val pagedOrganizationLanguageAssembler: PagedResourcesAssembler<OrganizationLanguageDto>,
) {
  @Operation(
    summary = "Get all languages in use by projects owned by specified organization",
    description = "Returns all languages in use by projects owned by specified organization",
  )
  @GetMapping("/languages")
  @UseDefaultPermissions
  fun getAllLanguagesInUse(
    @ParameterObject
    @SortDefault("base", direction = Sort.Direction.DESC)
    @SortDefault("name", direction = Sort.Direction.ASC)
    @SortDefault("tag", direction = Sort.Direction.ASC)
    pageable: Pageable,
    @RequestParam("search") search: String?,
    @RequestParam("projectIds") projectIds: List<Long>?,
    @PathVariable organizationId: Long,
  ): PagedModel<OrganizationLanguageModel> {
    val languages = languageService.getPagedByOrganization(organizationId, projectIds, pageable, search)
    return pagedOrganizationLanguageAssembler.toModel(languages, organizationLanguageModelAssembler)
  }

  @Operation(
    summary = "Get all base languages in use by projects owned by specified organization",
    description = "Returns all base languages in use by projects owned by specified organization",
  )
  @GetMapping("/base-languages")
  @UseDefaultPermissions
  fun getAllBaseLanguagesInUse(
    @ParameterObject
    @SortDefault("name", direction = Sort.Direction.ASC)
    @SortDefault("tag", direction = Sort.Direction.ASC)
    pageable: Pageable,
    @RequestParam("search") search: String?,
    @RequestParam("projectIds") projectIds: List<Long>?,
    @PathVariable organizationId: Long,
  ): PagedModel<OrganizationLanguageModel> {
    val languages = languageService.getBasePagedByOrganization(organizationId, projectIds, pageable, search)
    return pagedOrganizationLanguageAssembler.toModel(languages, organizationLanguageModelAssembler)
  }
}
