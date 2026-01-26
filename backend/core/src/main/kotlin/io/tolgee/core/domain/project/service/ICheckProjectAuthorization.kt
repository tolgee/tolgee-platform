package io.tolgee.core.domain.project.service

import io.tolgee.core.concepts.types.OutputMarker
import io.tolgee.core.domain.project.data.ProjectId
import io.tolgee.core.domain.user.data.UserId
import io.tolgee.service.security.PermissionService
import org.springframework.stereotype.Service

/**
 * Interface for checking if a user has authorization to access a project.
 *
 * This is used by scenarios to verify project access as part of authorization,
 * separate from checking whether the project exists.
 */
interface ICheckProjectAuthorization {
  sealed interface Output : OutputMarker {
    /**
     * User has permission to access the project.
     */
    data object Authorized : Output

    /**
     * User does not have permission to access the project.
     * This maps to HTTP 404 (project_not_found) to avoid leaking project existence.
     */
    data object Denied : Output
  }

  /**
   * Check if the user has any permission on the specified project.
   *
   * @param userId The user to check permissions for
   * @param projectId The project to check access to
   * @return [Output.Authorized] if user has access, [Output.Denied] otherwise
   */
  fun hasAccess(userId: UserId, projectId: ProjectId): Output
}

@Service
class ICheckProjectAuthorizationImpl(
  private val permissionService: PermissionService,
) : ICheckProjectAuthorization {
  override fun hasAccess(userId: UserId, projectId: ProjectId): ICheckProjectAuthorization.Output {
    val scopes = permissionService.getProjectPermissionScopesNoApiKey(projectId.value, userId.value)
    return if (scopes.isNullOrEmpty()) {
      ICheckProjectAuthorization.Output.Denied
    } else {
      ICheckProjectAuthorization.Output.Authorized
    }
  }
}
