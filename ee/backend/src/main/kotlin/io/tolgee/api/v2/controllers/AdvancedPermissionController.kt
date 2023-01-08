package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.request.project.SetPermissionLanguageParams
import io.tolgee.exceptions.BadRequestException
import io.tolgee.facade.ProjectPermissionFacade
import io.tolgee.model.enums.Scope
import io.tolgee.security.NeedsSuperJwtToken
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.EePermissionService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/projects/")
@Tag(name = "User invitations to project")
class AdvancedPermissionController(
  private val projectPermissionFacade: ProjectPermissionFacade,
  private val eePermissionService: EePermissionService,
  private val projectHolder: ProjectHolder
) {
  @PutMapping("{projectId}/users/{userId}/set-permissions")
  @AccessWithProjectPermission(Scope.ADMIN)
  @Operation(summary = "Sets user's direct permission")
  @NeedsSuperJwtToken
  fun setUsersPermissions(
    @PathVariable("projectId") projectId: Long,
    @PathVariable("userId") userId: Long,
    @Schema(
      description = "Permitted scopes for specific user",
      example = """["translations.view", "translations.edit"]"""
    )
    @RequestParam scopes: List<String>,
    @ParameterObject params: SetPermissionLanguageParams
  ) {
    val parsedScopes = parsedScopes(scopes)
    projectPermissionFacade.checkNotCurrentUser(userId)
    eePermissionService.setUserDirectPermission(
      projectId = projectId,
      userId = userId,
      languages = projectPermissionFacade.getLanguages(params, projectHolder.project.id),
      scopes = parsedScopes
    )
  }

  private fun parsedScopes(scopes: List<String>): Set<Scope> {
    return scopes.map { stringScope ->
      Scope.values().find { it.value == stringScope } ?: throw BadRequestException(
        Message.SCOPE_NOT_FOUND,
        listOf(stringScope)
      )
    }.toSet()
  }
}
