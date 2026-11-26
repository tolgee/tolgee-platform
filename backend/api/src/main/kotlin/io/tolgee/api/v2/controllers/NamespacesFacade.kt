package io.tolgee.api.v2.controllers

import io.tolgee.api.mcp.McpFacade
import io.tolgee.model.key.Namespace
import io.tolgee.security.ProjectHolder
import io.tolgee.service.key.NamespaceService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class NamespacesFacade(
  private val namespaceService: NamespaceService,
  private val projectHolder: ProjectHolder,
) : McpFacade {
  fun getAllInProject(
    pageable: Pageable,
    search: String?,
  ): Page<Namespace> = namespaceService.getAllInProject(projectHolder.project.id, pageable, search)

  fun getAllInProject(): List<Namespace> = namespaceService.getAllInProject(projectHolder.project.id)
}
