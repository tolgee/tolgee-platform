package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.dtos.sso.SsoTenantDto
import io.tolgee.dtos.sso.toDto
import io.tolgee.ee.api.v2.hateoas.assemblers.SsoTenantAssembler
import io.tolgee.hateoas.ee.SsoTenantModel
import io.tolgee.ee.data.CreateProviderRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.SsoTenant
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.service.TenantService
import io.tolgee.service.organization.OrganizationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/sso"])
@Tag(name = "Sso Tenant", description = "SSO Tenant configuration authentication")
class SsoProviderController(
  private val authenticationFacade: AuthenticationFacade,
  private val tenantService: TenantService,
  private val ssoTenantAssembler: SsoTenantAssembler,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val organizationService: OrganizationService,
  private val properties: TolgeeProperties,
) {
  @RequiresOrganizationRole(role = OrganizationRoleType.OWNER)
  @PutMapping("")
  @Operation(
    summary = "Set SSO Tenant configuration for organization",
  )
  @RequiresSuperAuthentication
  fun setProvider(
    @RequestBody @Valid request: CreateProviderRequest,
    @PathVariable organizationId: Long,
  ): SsoTenantModel {
    validateProvider(request)

    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = organizationId,
      Feature.SSO,
    )

    val isAdmin = authenticationFacade.authenticatedUser.role == UserAccount.Role.ADMIN
    val organization = organizationService.get(organizationId)
    return ssoTenantAssembler.toModel(
      tenantService.createOrUpdate(request.toDto(), organization, allowChangeDomain = isAdmin).toDto(),
    )
  }

  @RequiresOrganizationRole(role = OrganizationRoleType.OWNER)
  @GetMapping("")
  @Operation(
    summary = "Get SSO Tenant configuration for organization",
  )
  @RequiresSuperAuthentication
  fun findProvider(
    @PathVariable organizationId: Long,
  ): SsoTenantModel? {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = organizationId,
      Feature.SSO,
    )
    val tenant: SsoTenant
    try {
      tenant = tenantService.getTenant(organizationId)
    } catch (_: NotFoundException) {
      return null
    }
    return ssoTenantAssembler.toModel(tenant.toDto())
  }

  private fun validateProvider(req: CreateProviderRequest) {
    if (!req.enabled) {
      return
    }

    listOf(
      req::clientId,
      req::clientSecret,
      req::authorizationUri,
      req::domain,
      req::tokenUri,
    ).forEach {
      if (it.get().isBlank()) {
        throw BadRequestException(Message.CANNOT_SET_SSO_PROVIDER_MISSING_FIELDS, listOf(it.name))
      }
    }
  }

  private fun CreateProviderRequest.toDto(): SsoTenantDto {
    return SsoTenantDto(
      authorizationUri = this.authorizationUri,
      clientId = this.clientId,
      clientSecret = this.clientSecret,
      tokenUri = this.tokenUri,
      force = this.force,
      enabled = this.enabled,
      domain = this.domain,
    )
  }
}
