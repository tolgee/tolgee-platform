package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.organization.OrganizationModelAssembler
import io.tolgee.controllers.IController
import io.tolgee.model.views.OrganizationView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.OrganizationService
import io.tolgee.service.SecurityService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.tags.Tag as OpenApiTag

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/administration"
  ]
)
@OpenApiTag(name = "Admin", description = "Server administration")
class AdministrationController(
  private val organizationService: OrganizationService,
  private val pagedOrganizationResourcesAssembler: PagedResourcesAssembler<OrganizationView>,
  private val organizationModelAssembler: OrganizationModelAssembler,
  private val securityService: SecurityService,
  private val authenticationFacade: AuthenticationFacade
) : IController {

  @GetMapping(value = ["/organizations"])
  @Operation(summary = "Get all server organizations")
  fun getOrganizations(
    @ParameterObject @SortDefault(sort = ["name"]) pageable: Pageable,
    search: String? = null
  ): PagedModel<OrganizationModel> {
    securityService.checkUserIsServerAdmin()
    val organizations = organizationService.findAllPaged(pageable, search, authenticationFacade.userAccount.id)
    return pagedOrganizationResourcesAssembler.toModel(organizations, organizationModelAssembler)
  }
}
