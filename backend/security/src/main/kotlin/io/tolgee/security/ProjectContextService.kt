package io.tolgee.security

import io.tolgee.activity.ActivityHolder
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.dtos.cacheable.isSupporterOrAdmin
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.exceptions.ProjectNotFoundException
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
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
   * A guest never bypasses anything, and every denial is "sign in" for a guest.
   */
  fun setup(
    project: ProjectDto,
    requiredScopes: Array<Scope>?,
    useDefaultPermissions: Boolean,
    isWriteOperation: Boolean,
  ) {
    val userId = authenticationFacade.authenticatedUserOrGuest.id
    var bypassed = false

    // Before the scope gate: a credential pointed at a project it does not cover is told so, rather than being told
    // the project does not exist because its permitted-scope set came back empty.
    validateScopedCredential(project, userId)

    if (useDefaultPermissions || requiredScopes != null) {
      val scopes = securityService.getCurrentPermittedScopes(project.id)

      if (scopes.isEmpty()) {
        if (!canBypass(isWriteOperation)) {
          logger.debug(
            "Rejecting access to proj#{} for user#{} - No view permissions",
            project.id,
            userId,
          )

          if (!canBypassForReadOnly()) {
            // Security consideration: if the user cannot see the project, pretend it does not exist.
            deny(ProjectNotFoundException(project.id))
          }

          // Admin access for read-only operations is allowed, but it's not enough for the current operation.
          deny(PermissionException())
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

            deny(
              PermissionException(
                Message.OPERATION_NOT_PERMITTED,
                missing.map { it.value },
              ),
            )
          }

          bypassed = true
        }
      }
    }

    if (bypassed) {
      logger.info(
        "Use of admin privileges: user#{} failed local security checks for proj#{}",
        userId,
        project.id,
      )
    }

    populateHolders(project)
  }

  private fun populateHolders(project: ProjectDto) {
    projectHolder.project = project
    activityHolder.activityRevision.projectId = project.id
    organizationHolder.organization =
      organizationService.findDto(project.organizationOwnerId)
        ?: throw NotFoundException(Message.ORGANIZATION_NOT_FOUND)
  }

  private fun validateScopedCredential(
    project: ProjectDto,
    userId: Long,
  ) {
    val credential = authenticationFacade.scopedCredential ?: return
    if (credential.coversProject(project.id)) return

    // The user's own access answers first: telling a credential "wrong project" for a project its user cannot see
    // would turn any scoped credential into an instance-wide project-existence oracle.
    if (securityService.getProjectPermissionScopesNoApiKey(project.id, userId).isNullOrEmpty()) {
      throw ProjectNotFoundException(project.id)
    }
    throw PermissionException(credential.projectMismatchMessage)
  }

  private val canUseAdminPermissions
    get() = !authenticationFacade.isScopedCredential

  private fun canBypass(isWriteOperation: Boolean): Boolean {
    if (authenticationFacade.isGuest) return false
    if (!canUseAdminPermissions) return false
    if (authenticationFacade.authenticatedUser.isAdmin()) return true
    return !isWriteOperation && canBypassForReadOnly()
  }

  private fun canBypassForReadOnly(): Boolean {
    if (authenticationFacade.isGuest) return false
    return canUseAdminPermissions && authenticationFacade.authenticatedUser.isSupporterOrAdmin()
  }

  private fun deny(exception: Exception): Nothing {
    if (authenticationFacade.isGuest) throw AuthenticationException(Message.UNAUTHENTICATED)
    throw exception
  }
}
