package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.LlmProviderDto
import io.tolgee.dtos.request.llmProvider.LlmProviderRequest
import io.tolgee.ee.api.v2.hateoas.assemblers.LlmProviderModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.LlmProviderSimpleModelAssembler
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.hateoas.llmProvider.LlmProviderModel
import io.tolgee.hateoas.llmProvider.LlmProviderSimpleModel
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import jakarta.validation.Valid
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/llm-providers"])
@Tag(name = "Llm providers")
@OpenApiOrderExtension(6)
class LlmProviderController(
  private val providerService: LlmProviderService,
  private val providerModelAssembler: LlmProviderModelAssembler,
  private val providerSimpleModelAssembler: LlmProviderSimpleModelAssembler,
) {
  @GetMapping("all-available")
  @UseDefaultPermissions
  @Operation(
    summary = "Get all available llm providers",
    description = "Combines llm providers from organization-specific and server-configured",
  )
  fun getAvailableProviders(
    @PathVariable organizationId: Long,
  ): CollectionModel<LlmProviderSimpleModel> {
    val serverProviders = providerService.getAllServerProviders()
    val customProviders = providerService.getAll(organizationId)
    val existing = mutableSetOf<String>()
    val result = mutableListOf<LlmProviderDto>()
    customProviders.forEach {
      if (!existing.contains(it.name)) {
        result.add(it)
        existing.add(it.name)
      }
    }
    serverProviders.forEach {
      if (!existing.contains(it.name)) {
        result.add(it)
        existing.add(it.name)
      }
    }
    return providerSimpleModelAssembler.toCollectionModel(result)
  }

  @GetMapping("")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(summary = "Get all organization-specific providers")
  fun getAll(
    @PathVariable organizationId: Long,
  ): CollectionModel<LlmProviderModel> {
    val providers = providerService.getAll(organizationId)
    return providerModelAssembler.toCollectionModel(providers)
  }

  @GetMapping("server-providers")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(summary = "Get all server-configured providers")
  fun getServerProviders(
    @PathVariable organizationId: Long,
  ): CollectionModel<LlmProviderSimpleModel> {
    val providers = providerService.getAllServerProviders()
    return providerSimpleModelAssembler.toCollectionModel(providers)
  }

  @PostMapping("")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(summary = "Create organization-specific provider")
  fun createProvider(
    @PathVariable organizationId: Long,
    @RequestBody @Valid dto: LlmProviderRequest,
  ): LlmProviderModel {
    val result = providerService.createProvider(organizationId, dto)
    return providerModelAssembler.toModel(result)
  }

  @PutMapping("/{providerId:[0-9]+}")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(summary = "Update organization-specific provider")
  fun updateProvider(
    @PathVariable organizationId: Long,
    @PathVariable providerId: Long,
    @RequestBody @Valid dto: LlmProviderRequest,
  ): LlmProviderModel {
    val result = providerService.updateProvider(organizationId, providerId, dto)
    return providerModelAssembler.toModel(result)
  }

  @DeleteMapping("/{providerId:[0-9]+}")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(summary = "Delete organization-specific provider")
  fun deleteProvider(
    @PathVariable organizationId: Long,
    @PathVariable providerId: Long,
  ) {
    providerService.deleteProvider(organizationId, providerId)
  }
}
