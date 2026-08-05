package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import io.tolgee.repository.apps.AppInstallRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppEnablementService(
  private val appEnabledForProjectRepository: AppEnabledForProjectRepository,
  private val appInstallRepository: AppInstallRepository,
  private val appEnablementInserter: AppEnablementInserter,
  private val appAvailabilityService: AppAvailabilityService,
) {
  data class ProjectAppEnablement(
    val install: AppInstall,
    val enabled: Boolean,
  )

  @Transactional
  fun enable(
    project: Project,
    installId: Long,
    author: UserAccount,
  ): AppInstall {
    val orgId = project.organizationOwner.id
    val install = resolveEnableableInstall(orgId, installId)

    if (appEnabledForProjectRepository.findByProjectIdAndAppInstallId(project.id, install.id) == null) {
      try {
        appEnablementInserter.insert(install.id, project.id, author.id)
      } catch (e: DataIntegrityViolationException) {
        // Only a concurrent enable is idempotent. The same exception also covers an FK violation
        // (install or project deleted mid-flight), which must not be reported as success.
        appEnabledForProjectRepository.findByProjectIdAndAppInstallId(project.id, install.id)
          ?: throw e
      }
    }

    return install
  }

  /**
   * An install of a *different* organization stays indistinguishable from a missing one (404), so
   * this never confirms the existence of another tenant's install.
   */
  private fun resolveEnableableInstall(
    organizationId: Long,
    installId: Long,
  ): AppInstall {
    val owned = appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
    if (owned != null) return owned

    val native =
      appInstallRepository.findByOrganizationIsNullAndId(installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    if (!appAvailabilityService.isAvailableForOrganization(organizationId, native)) {
      throw BadRequestException(Message.APP_NOT_AVAILABLE_FOR_ORGANIZATION)
    }
    return native
  }

  @Transactional
  fun disable(
    projectId: Long,
    appInstallId: Long,
  ) {
    val existing =
      appEnabledForProjectRepository.findByProjectIdAndAppInstallId(projectId, appInstallId) ?: return
    appEnabledForProjectRepository.delete(existing)
  }

  @Transactional(readOnly = true)
  fun listAppsForProject(project: Project): List<ProjectAppEnablement> {
    val organizationId = project.organizationOwner.id
    val installs =
      appInstallRepository.findAllByOrganizationId(organizationId) +
        appAvailabilityService.listNativeInstallsForOrganization(organizationId)
    val enabledIds =
      appEnabledForProjectRepository
        .findAllByProjectId(project.id)
        .map { it.appInstall.id }
        .toSet()
    return installs.map { ProjectAppEnablement(it, it.id in enabledIds) }
  }

  @Transactional(readOnly = true)
  fun listEnabledInstallsForProject(projectId: Long): List<AppInstall> {
    return appEnabledForProjectRepository.findEnabledInstallsByProjectId(projectId)
  }

  @Transactional(readOnly = true)
  fun isEnabledForProject(
    projectId: Long,
    appInstallId: Long,
  ): Boolean {
    return appEnabledForProjectRepository.findByProjectIdAndAppInstallId(projectId, appInstallId) != null
  }

  @Transactional
  fun removeAllForAppInstall(appInstallId: Long) {
    appEnabledForProjectRepository.deleteByAppInstallId(appInstallId)
  }
}
