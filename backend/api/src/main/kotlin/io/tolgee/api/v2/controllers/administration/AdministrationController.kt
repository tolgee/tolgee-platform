package io.tolgee.api.v2.controllers.administration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.dtos.queryResults.organization.OrganizationView
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.organization.OrganizationModel
import io.tolgee.hateoas.organization.OrganizationModelAssembler
import io.tolgee.hateoas.userAccount.UserAccountModel
import io.tolgee.hateoas.userAccount.UserAccountModelAssembler
import io.tolgee.model.UserAccount
import io.tolgee.openApiDocs.OpenApiSelfHostedExtension
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.UserAccountService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/administration",
  ],
)
@Tag(
  name = "Server Administration",
  description =
    "**Only for self-hosted instances** \n\n" +
      "Managees global Tolgee Platform instance data e.g., user accounts and organizations.",
)
@OpenApiSelfHostedExtension
class AdministrationController(
  private val organizationService: OrganizationService,
  private val pagedOrganizationResourcesAssembler: PagedResourcesAssembler<OrganizationView>,
  private val organizationModelAssembler: OrganizationModelAssembler,
  private val authenticationFacade: AuthenticationFacade,
  private val userAccountService: UserAccountService,
  private val pagedResourcesAssembler: PagedResourcesAssembler<UserAccount>,
  private val userAccountModelAssembler: UserAccountModelAssembler,
  private val jwtService: JwtService,
) : IController {
  @GetMapping(value = ["/organizations"])
  @Operation(summary = "Get all server organizations")
  @RequiresSuperAuthentication
  fun getOrganizations(
    @ParameterObject
    @SortDefault(sort = ["name"])
    pageable: Pageable,
    search: String? = null,
  ): PagedModel<OrganizationModel> {
    val organizations =
      organizationService.findAllPaged(
        pageable = pageable,
        search = search,
        userId = authenticationFacade.authenticatedUser.id,
      )
    return pagedOrganizationResourcesAssembler.toModel(organizations, organizationModelAssembler)
  }

  @GetMapping(value = ["/users"])
  @Operation(summary = "Get all server users")
  @RequiresSuperAuthentication
  fun getUsers(
    @ParameterObject
    @SortDefault(sort = ["name"])
    pageable: Pageable,
    search: String? = null,
  ): PagedModel<UserAccountModel> {
    val users = userAccountService.findAllWithDisabledPaged(pageable, search)
    return pagedResourcesAssembler.toModel(users, userAccountModelAssembler)
  }

  @DeleteMapping(value = ["/users/{userId}"])
  @Operation(summary = "Delete user")
  @RequiresSuperAuthentication
  fun deleteUser(
    @PathVariable userId: Long,
  ) {
    if (userId == authenticationFacade.authenticatedUser.id) {
      throw BadRequestException(Message.CANNOT_DELETE_YOUR_OWN_ACCOUNT)
    }
    userAccountService.delete(userId)
  }

  @PutMapping(value = ["/users/{userId}/disable"])
  @Operation(
    summary = "Disable user",
    description =
      "Disables user account. User will not be able to log in, " +
        "but their " +
        "user data will be preserved, so you can enable the user later using the `enable` endpoint.",
  )
  @RequiresSuperAuthentication
  fun disableUser(
    @PathVariable userId: Long,
  ) {
    if (userId == authenticationFacade.authenticatedUser.id) {
      throw BadRequestException(Message.CANNOT_DISABLE_YOUR_OWN_ACCOUNT)
    }
    userAccountService.disable(userId)
  }

  @PutMapping(value = ["/users/{userId}/enable"])
  @Operation(summary = "Enable user", description = "Enables previously disabled user.")
  @RequiresSuperAuthentication
  fun enableUser(
    @PathVariable userId: Long,
  ) {
    userAccountService.enable(userId)
  }

  @PutMapping(value = ["/users/{userId:[0-9]+}/set-role/{role}"])
  @Operation(summary = "Set Role", description = "Set's the global role on the Tolgee Platform server.")
  @RequiresSuperAuthentication
  fun setRole(
    @PathVariable userId: Long,
    @PathVariable role: UserAccount.Role,
  ) {
    val user = userAccountService.get(userId)
    user.role = role
    userAccountService.save(user)
  }

  @GetMapping(value = ["/users/{userId:[0-9]+}/generate-token"])
  @Operation(
    summary = "Geneate user's JWT token",
    description =
      "Generates a JWT token for the user with provided ID. This is useful, when need to debug of the " +
        "user's account. Or when an operation is required to be executed on behalf of the user.",
  )
  @RequiresSuperAuthentication
  fun generateUserToken(
    @PathVariable userId: Long,
  ): String {
    val isAlreadyImpersonating = authenticationFacade.actingUser != null
    if (isAlreadyImpersonating) {
      // We don't want to recreate the Inception movie here
      throw BadRequestException(Message.ALREADY_IMPERSONATING_USER)
    }

    val actingUser = authenticationFacade.authenticatedUser
    val user = userAccountService.getDto(userId)
    if (user.isAdmin() && !actingUser.isAdmin()) {
      // We don't allow impersonation of admin by supporters
      throw BadRequestException(Message.IMPERSONATION_OF_ADMIN_BY_SUPPORTER_NOT_ALLOWED)
    }
    return jwtService.emitImpersonationToken(user.id)
  }
}
