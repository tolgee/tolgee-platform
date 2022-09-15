package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.organization.OrganizationModelAssembler
import io.tolgee.api.v2.hateoas.user_account.UserAccountModel
import io.tolgee.api.v2.hateoas.user_account.UserAccountModelAssembler
import io.tolgee.controllers.IController
import io.tolgee.model.UserAccount
import io.tolgee.model.views.OrganizationView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.JwtTokenProvider
import io.tolgee.service.OrganizationService
import io.tolgee.service.SecurityService
import io.tolgee.service.UserAccountService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.tags.Tag as OpenApiTag

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
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
  private val authenticationFacade: AuthenticationFacade,
  private val userAccountService: UserAccountService,
  private val pagedResourcesAssembler: PagedResourcesAssembler<UserAccount>,
  private val userAccountModelAssembler: UserAccountModelAssembler,
  private val jwtTokenProvider: JwtTokenProvider
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

  @GetMapping(value = ["/users"])
  @Operation(summary = "Get all server users")
  fun getUsers(
    @ParameterObject @SortDefault(sort = ["name"]) pageable: Pageable,
    search: String? = null
  ): PagedModel<UserAccountModel> {
    securityService.checkUserIsServerAdmin()
    val users = userAccountService.findAllPaged(pageable, search)
    return pagedResourcesAssembler.toModel(users, userAccountModelAssembler)
  }

  @PutMapping(value = ["/users/{userId:[0-9]+}/set-role/{role}"])
  @Operation(summary = "")
  fun setRole(
    @PathVariable userId: Long,
    @PathVariable role: UserAccount.Role
  ) {
    securityService.checkUserIsServerAdmin()
    val user = userAccountService.get(userId)
    user.role = role
    userAccountService.save(user)
  }

  @GetMapping(value = ["/users/{userId:[0-9]+}/generate-token"])
  @Operation(summary = "Get all server users")
  fun generateUserToken(
    @PathVariable userId: Long,
  ): String {
    securityService.checkUserIsServerAdmin()
    val user = userAccountService.get(userId)
    val token = jwtTokenProvider.generateToken(user.id, isSuper = true)
    return token.toString()
  }
}
