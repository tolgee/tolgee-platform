package io.tolgee.security

import io.tolgee.activity.ActivityHolder
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.dtos.cacheable.isSupporterOrAdmin
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.exceptions.ProjectNotFoundException
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AppAuthentication
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.SecurityService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ProjectContextService(
  private val authenticationFacade: AuthenticationFacade,
  private val projectService: ProjectService,
  private val organizationService: OrganizationService,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val organizationHolder: OrganizationHolder,
  private val activityHolder: ActivityHolder,
  private val appEnablementService: AppEnablementService,
  private val permissionService: PermissionService,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  fun setup(
    projectId: Long,
    requiredScopes: Array<Scope>?,
    useDefaultPermissions: Boolean,
    isWriteOperation: Boolean,
  ) {
    val project =
      projectService.findDto(projectId)
        ?: throw NotFoundException(Message.PROJECT_NOT_FOUND)

    setup(project, requiredScopes, useDefaultPermissions, isWriteOperation)
  }

  /**
   * Sets up project context with admin bypass support.
   *
   * When a permission check fails, admins and supporters (for read-only operations)
   * can bypass the check. If the user has zero access and can't even bypass for
   * read-only, a [ProjectNotFoundException] is thrown to hide project existence.
   */
  fun setup(
    project: ProjectDto,
    requiredScopes: Array<Scope>?,
    useDefaultPermissions: Boolean,
    isWriteOperation: Boolean,
  ) {
    bindAppToProject(project)

    val userId = authenticationFacade.authenticatedUser.id
    var bypassed = false

    if (useDefaultPermissions || requiredScopes != null) {
      val scopes = securityService.getCurrentPermittedScopes(project.id)

      if (scopes.isEmpty()) {
        if (!canBypass(isWriteOperation)) {
          logger.debug(
            "Rejecting access to proj#{} for user#{} - No view permissions",
            project.id,
            userId,
          )

          if (authenticationFacade.isAppAuth) {
            // The user reached this project through an enabled app in a project they belong to; the
            // empty scope set means the app's grantedScopes don't intersect the user's project
            // scopes. Surface a 403 with the required scopes — hiding project existence is the wrong
            // threat model here.
            throw PermissionException(
              Message.OPERATION_NOT_PERMITTED,
              requiredScopes?.map { it.value } ?: emptyList(),
            )
          }

          if (!canBypassForReadOnly()) {
            // Security consideration: if the user cannot see the project, pretend it does not exist.
            throw ProjectNotFoundException(project.id)
          }

          // Admin access for read-only operations is allowed, but it's not enough for the current operation.
          throw PermissionException()
        }

        bypassed = true
      }

      if (requiredScopes != null) {
        val missing = requiredScopes.toSet() - scopes
        if (missing.isNotEmpty()) {
          if (!canBypass(isWriteOperation)) {
            logger.debug(
              "Rejecting access to proj#{} for user#{} - Insufficient permissions",
              project.id,
              userId,
            )

            throw PermissionException(
              Message.OPERATION_NOT_PERMITTED,
              missing.map { it.value },
            )
          }

          bypassed = true
        }
      }
    }

    validatePak(project)

    if (bypassed) {
      logger.info(
        "Use of admin privileges: user#{} failed local security checks for proj#{}",
        userId,
        project.id,
      )
    }

    populateHolders(project)
  }

  /**
   * Binds an app authentication to the request's project. Until this runs the app has no project it
   * may act on, so [SecurityService.getCurrentPermittedScopes] resolves to nothing — every project
   * route therefore has to pass through here, and one that never reaches it denies rather than
   * silently skipping the enablement check.
   */
  private fun bindAppToProject(project: ProjectDto) {
    if (!authenticationFacade.isAppAuth) return
    val appAuth = authenticationFacade.appAuthentication

    if (appAuth.tokenProjectId != null && appAuth.tokenProjectId != project.id) {
      throw PermissionException(Message.APP_NOT_ENABLED_FOR_PROJECT)
    }

    if (!appEnablementService.isEnabledForProject(project.id, appAuth.appInstall.id)) {
      throw PermissionException(Message.APP_NOT_ENABLED_FOR_PROJECT)
    }

    checkActingAsUserIsProjectMember(appAuth, project.id)

    appAuth.boundProjectId = project.id
  }

  /**
   * An install may narrow itself to a project member, never widen itself to a stranger.
   */
  private fun checkActingAsUserIsProjectMember(
    appAuth: AppAuthentication,
    projectId: Long,
  ) {
    val actingAs = appAuth.actingAsUserAccount ?: return
    val scopes = permissionService.getProjectPermissionScopesNoApiKey(projectId, actingAs.id)
    if (scopes.isNullOrEmpty()) {
      throw PermissionException(Message.APP_ACTING_AS_USER_NOT_PROJECT_MEMBER)
    }
  }

  private fun populateHolders(project: ProjectDto) {
    projectHolder.project = project
    activityHolder.activityRevision.projectId = project.id
    organizationHolder.organization =
      organizationService.findDto(project.organizationOwnerId)
        ?: throw NotFoundException(Message.ORGANIZATION_NOT_FOUND)
  }

  private fun validatePak(project: ProjectDto) {
    if (!authenticationFacade.isProjectApiKeyAuth) return
    val pak = authenticationFacade.projectApiKey
    if (project.id != pak.projectId) {
      throw PermissionException(Message.PAK_CREATED_FOR_DIFFERENT_PROJECT)
    }
  }

  private val canUseAdminPermissions
    get() = !authenticationFacade.isProjectApiKeyAuth && !authenticationFacade.isAppAuth

  private fun canBypass(isWriteOperation: Boolean): Boolean {
    if (!canUseAdminPermissions) return false
    if (authenticationFacade.authenticatedUser.isAdmin()) return true
    return !isWriteOperation && canBypassForReadOnly()
  }

  private fun canBypassForReadOnly(): Boolean {
    return canUseAdminPermissions && authenticationFacade.authenticatedUser.isSupporterOrAdmin()
  }
}
