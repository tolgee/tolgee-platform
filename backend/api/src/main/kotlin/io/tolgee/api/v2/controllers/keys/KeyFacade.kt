package io.tolgee.api.v2.controllers.keys

import io.tolgee.api.mcp.McpFacade
import io.tolgee.api.mcp.checkValid
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.KeyView
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.dtos.request.translation.ImportKeysDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.key.Key
import io.tolgee.security.ProjectHolder
import io.tolgee.service.key.KeySearchResultView
import io.tolgee.service.key.KeyService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.security.SecurityService
import io.tolgee.util.withoutSort
import jakarta.validation.Validator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class KeyFacade(
  private val keyService: KeyService,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val validator: Validator,
) : McpFacade {
  fun getPaged(
    branch: String?,
    pageable: Pageable,
  ): Page<KeyView> {
    projectFeatureGuard.checkIfUsed(Feature.BRANCHING, branch)
    return keyService.getPaged(projectHolder.project.id, branch, pageable)
  }

  fun getView(id: Long): KeyView = keyService.getView(projectHolder.project.id, id)

  fun searchKeys(
    search: String,
    languageTag: String?,
    branch: String?,
    pageable: Pageable,
  ): Page<KeySearchResultView> {
    projectFeatureGuard.checkIfUsed(Feature.BRANCHING, branch)
    languageTag?.let {
      securityService.checkLanguageViewPermissionByTag(projectHolder.project.id, listOf(it))
    }
    projectHolder.projectEntity.baseLanguage?.let {
      securityService.checkLanguageViewPermissionByTag(projectHolder.project.id, listOf(it.tag))
    }
    return keyService.searchKeys(search, languageTag, projectHolder.project, branch, pageable.withoutSort)
  }

  fun importKeys(
    dto: ImportKeysDto,
    branch: String?,
  ) {
    validator.checkValid(dto)
    projectFeatureGuard.checkIfUsed(Feature.BRANCHING, branch)
    keyService.importKeys(dto.keys, projectHolder.projectEntity, branch)
  }

  fun find(
    name: String,
    namespace: String?,
    branch: String?,
  ): Key? {
    projectFeatureGuard.checkIfUsed(Feature.BRANCHING, branch)
    return keyService.find(projectHolder.project.id, name, namespace, branch)
  }

  fun edit(
    id: Long,
    dto: EditKeyDto,
  ): Key {
    validator.checkValid(dto)
    val key = keyService.findOptional(id).orElseThrow { NotFoundException() }
    keyService.checkInProject(key, projectHolder.project.id)
    checkNamespaceFeature(dto.namespace)
    projectFeatureGuard.checkIfUsed(Feature.BRANCHING, dto.branch)
    return keyService.edit(id, dto)
  }

  fun delete(ids: Set<Long>) {
    keyService.findAllWithProjectsAndMetas(ids).forEach {
      keyService.checkInProject(it, projectHolder.project.id)
      securityService.checkBranchModify(it)
    }
    keyService.softDeleteMultiple(ids)
  }

  fun resolveKeysByName(
    keyNames: List<String>,
    namespace: String?,
    branch: String?,
  ): Pair<List<Key>, List<String>> {
    projectFeatureGuard.checkIfUsed(Feature.BRANCHING, branch)
    return keyService.resolveKeysByName(projectHolder.project.id, keyNames, namespace, branch)
  }

  fun checkNamespaceFeature(namespace: String?) {
    if (projectHolder.projectEntity.useNamespaces) return
    if (namespace == null) return
    throw ValidationException(Message.NAMESPACE_CANNOT_BE_USED_WHEN_FEATURE_IS_DISABLED)
  }
}
