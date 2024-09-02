package io.tolgee.ee.api.v2.controllers

import io.tolgee.ee.data.CreateProviderRequest
import io.tolgee.ee.model.Tenant
import io.tolgee.ee.service.TenantService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/providers")
class SsoProviderController(private val tenantService: TenantService) {
  @PostMapping("")
  fun addProvider(
    @RequestBody request: CreateProviderRequest,
  ): Tenant {
    return tenantService.save(request)
  }
}
