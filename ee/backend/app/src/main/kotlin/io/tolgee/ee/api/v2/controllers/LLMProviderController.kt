package io.tolgee.ee.api.v2.controllers

import io.tolgee.dtos.request.llmProvider.LLMProviderRequest
import io.tolgee.ee.api.v2.hateoas.assemblers.LLMProviderModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.LLMProviderSimpleModelAssembler
import io.tolgee.ee.service.LLMProviderService
import io.tolgee.hateoas.NonPagedModel
import io.tolgee.hateoas.llmProvider.*
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/llm-providers"])
@OpenApiOrderExtension(6)
class LLMProviderController(
  private val providerService: LLMProviderService,
  private val providerModelAssembler: LLMProviderModelAssembler,
  private val providerSimpleModelAssembler: LLMProviderSimpleModelAssembler,
) {
  @GetMapping("all-available")
  @UseDefaultPermissions
  fun getAvailableProviders(
    @PathVariable organizationId: Long,
  ): NonPagedModel<LLMProviderSimpleModel> {
    val serverProviders = providerService.getAllServerProviders()
    val customProviders = providerService.getAll(organizationId)
    val existing = mutableSetOf<String>()
    val result = mutableListOf<LLMProviderSimpleModel>()
    customProviders.forEach {
      if (!existing.contains(it.name)) {
        result.add(providerSimpleModelAssembler.toModel(it))
        existing.add(it.name)
      }
    }
    serverProviders.forEach {
      if (!existing.contains(it.name)) {
        result.add(providerSimpleModelAssembler.toModel(it))
        existing.add(it.name)
      }
    }
    return NonPagedModel(result)
  }

  @GetMapping("")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  fun getAll(
    @PathVariable organizationId: Long,
  ): NonPagedModel<LLMProviderModel> {
    val providers = providerService.getAll(organizationId)
    return NonPagedModel(providers.map { providerModelAssembler.toModel(it) })
  }

  @GetMapping("server-providers")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  fun getServerProviders(
    @PathVariable organizationId: Long,
  ): NonPagedModel<LLMProviderModel> {
    val providers = providerService.getAllServerProviders()
    return NonPagedModel(providers.map { providerModelAssembler.toModel(it) })
  }

  @PostMapping("")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  fun createProvider(
    @PathVariable organizationId: Long,
    @RequestBody @Valid dto: LLMProviderRequest,
  ): LLMProviderModel {
    val result = providerService.createProvider(organizationId, dto)
    return providerModelAssembler.toModel(result)
  }

  @PutMapping("/{providerId:[0-9]+}")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  fun updateProvider(
    @PathVariable organizationId: Long,
    @PathVariable providerId: Long,
    @RequestBody @Valid dto: LLMProviderRequest,
  ): LLMProviderModel {
    val result = providerService.updateProvider(organizationId, providerId, dto)
    return providerModelAssembler.toModel(result)
  }

  @DeleteMapping("/{providerId:[0-9]+}")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  fun deleteProvider(
    @PathVariable organizationId: Long,
    @PathVariable providerId: Long,
  ) {
    providerService.deleteProvider(organizationId, providerId)
  }
}
