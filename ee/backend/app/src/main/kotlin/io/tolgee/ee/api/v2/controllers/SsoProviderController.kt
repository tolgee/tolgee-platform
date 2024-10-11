package io.tolgee.ee.api.v2.controllers

import io.tolgee.ee.api.v2.hateoas.assemblers.SsoTenantAssembler
import io.tolgee.ee.data.CreateProviderRequest
import io.tolgee.ee.data.toDto
import io.tolgee.ee.service.TenantService
import io.tolgee.hateoas.ee.SsoTenantModel
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresOrganizationRole
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/{organizationId:[0-9]+}/sso/provider"])
class SsoProviderController(
  private val tenantService: TenantService,
  private val ssoTenantAssembler: SsoTenantAssembler,
) {
  @RequiresOrganizationRole(role = OrganizationRoleType.OWNER)
  @PutMapping("")
  @RequiresSuperAuthentication
  fun setProvider(
    @RequestBody @Valid request: CreateProviderRequest,
    @PathVariable organizationId: Long,
  ): SsoTenantModel = ssoTenantAssembler.toModel(tenantService.saveOrUpdate(request, organizationId).toDto())

  @RequiresOrganizationRole(role = OrganizationRoleType.OWNER)
  @GetMapping("")
  @RequiresSuperAuthentication
  fun getProvider(
    @PathVariable organizationId: Long,
  ): SsoTenantModel = ssoTenantAssembler.toModel(tenantService.getTenant(organizationId).toDto())
}
