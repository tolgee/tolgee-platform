package io.tolgee.ee.api.v2.controllers

import io.tolgee.ee.data.CreateProviderRequest
import io.tolgee.ee.data.TenantDto
import io.tolgee.ee.data.toDto
import io.tolgee.ee.model.Tenant
import io.tolgee.ee.service.TenantService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authorization.RequiresOrganizationRole
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/{organizationId:[0-9]+}/sso/provider"])
class SsoProviderController(
  private val tenantService: TenantService,
) {
  @RequiresOrganizationRole(role = OrganizationRoleType.OWNER)
  @PostMapping("")
  fun addProvider(
    @RequestBody request: CreateProviderRequest,
    @PathVariable organizationId: Long,
  ): Tenant = tenantService.saveOrUpdate(request, organizationId)

  @RequiresOrganizationRole(role = OrganizationRoleType.OWNER)
  @GetMapping("")
  fun getProvider(
    @PathVariable organizationId: Long,
  ): TenantDto? = tenantService.findTenant(organizationId)?.toDto()
}
