package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.key.namespace.NamespaceModel
import io.tolgee.api.v2.hateoas.key.namespace.NamespaceModelAssembler
import io.tolgee.api.v2.hateoas.key.namespace.UsedNamespaceModel
import io.tolgee.api.v2.hateoas.key.namespace.UsedNamespaceModelAssembler
import io.tolgee.controllers.IController
import io.tolgee.model.key.Namespace
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.key.NamespaceService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/",
  ]
)
@Tag(name = "Namespaces", description = "Manipulates key namespaces")
class NamespaceController(
  private val namespaceService: NamespaceService,
  private val projectHolder: ProjectHolder,
  private val namespaceModelAssembler: NamespaceModelAssembler,
  private val pagedResourcesAssembler: PagedResourcesAssembler<Namespace>,
  private val usedNamespaceModelAssembler: UsedNamespaceModelAssembler
) : IController {

  @GetMapping(value = ["namespaces"])
  @Operation(summary = "Returns all project namespaces")
  @AccessWithAnyProjectPermission
  @AccessWithApiKey
  fun getAllNamespaces(
    @ParameterObject @SortDefault("id") pageable: Pageable
  ): PagedModel<NamespaceModel> {
    val namespaces = namespaceService.getAllInProject(projectHolder.project.id, pageable)
    return pagedResourcesAssembler.toModel(namespaces, namespaceModelAssembler)
  }

  @GetMapping(value = ["used-namespaces"])
  @Operation(summary = "Returns all used project namespaces. Response contains default (null) namespace if used.")
  @AccessWithAnyProjectPermission
  @AccessWithApiKey
  fun getUsedNamespaces(): CollectionModel<UsedNamespaceModel> {
    val namespaces = namespaceService.getAllInProject(projectHolder.project.id)
      .map { it.id as Long? to it.name as String? }.toMutableList()
    val isDefaultUsed = namespaceService.isDefaultUsed(projectHolder.project.id)
    if (isDefaultUsed) {
      namespaces.add(0, null to null)
    }
    return usedNamespaceModelAssembler.toCollectionModel(namespaces)
  }
}
