package io.tolgee.security

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.authentication.AppAuthentication
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Component

@Component
class AppProjectContextBinder(
  private val authenticationFacade: AuthenticationFacade,
  private val appEnablementService: AppEnablementService,
  private val permissionService: PermissionService,
  private val userAccountService: UserAccountService,
) {
  fun bind(project: ProjectDto) {
    if (!authenticationFacade.isAppAuth) return
    val appAuth = authenticationFacade.appAuthentication
    if (appAuth.isAppLevel) return

    if (!appEnablementService.isEnabledForProject(project.id, appAuth.appInstall.id)) {
      if (userKnowsProject(appAuth, project)) throw PermissionException(Message.APP_NOT_ENABLED_FOR_PROJECT)
      throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
    }

    checkActingAsUserIsProjectMember(appAuth, project.id)

    appAuth.boundProjectId = project.id
  }

  private fun userKnowsProject(
    appAuth: AppAuthentication,
    project: ProjectDto,
  ): Boolean {
    if (appAuth.isInstallContext) return false
    if (project.organizationOwnerId != appAuth.appInstall.organizationId) return false
    return !permissionService.getProjectPermissionScopesNoApiKey(project.id, appAuth.principal.id).isNullOrEmpty()
  }

  private fun checkActingAsUserIsProjectMember(
    appAuth: AppAuthentication,
    projectId: Long,
  ) {
    val actsForUserId = appAuth.actsForUserId ?: return
    userAccountService.findDto(actsForUserId)
      ?: throw PermissionException(Message.APP_ACTING_AS_USER_NOT_PROJECT_MEMBER)
    val scopes = permissionService.getProjectPermissionScopesNoApiKey(projectId, actsForUserId)
    if (scopes.isNullOrEmpty()) {
      throw PermissionException(Message.APP_ACTING_AS_USER_NOT_PROJECT_MEMBER)
    }
  }
}
