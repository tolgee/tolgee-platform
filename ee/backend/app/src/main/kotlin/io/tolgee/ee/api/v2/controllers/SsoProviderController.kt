package io.tolgee.ee.api.v2.controllers

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.api.v2.hateoas.assemblers.SsoTenantAssembler
import io.tolgee.ee.data.CreateProviderRequest
import io.tolgee.ee.data.toDto
import io.tolgee.ee.service.sso.TenantService
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.ee.SsoTenantModel
import io.tolgee.model.SsoTenant
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.service.organization.OrganizationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/sso"])
class SsoProviderController(
  private val tenantService: TenantService,
  private val ssoTenantAssembler: SsoTenantAssembler,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val organizationService: OrganizationService,
) {
  @RequiresOrganizationRole(role = OrganizationRoleType.OWNER)
  @PutMapping("")
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

    val organization = organizationService.get(organizationId)
    return ssoTenantAssembler.toModel(tenantService.createOrUpdate(request, organization).toDto())
  }

  @RequiresOrganizationRole(role = OrganizationRoleType.OWNER)
  @GetMapping("")
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

    // FIXME: Should we validate URIs and domains if they are real?

    listOf(
      CreateProviderRequest::clientId,
      CreateProviderRequest::clientSecret,
      CreateProviderRequest::authorizationUri,
      CreateProviderRequest::domain,
      // CreateProviderRequest::jwkSetUri,
      CreateProviderRequest::tokenUri,
    ).forEach {
      if (it.get(req).isBlank()) {
        throw BadRequestException(Message.CANNOT_SET_SSO_PROVIDER_MISSING_FIELDS, listOf(it.name))
      }
    }
  }
}
