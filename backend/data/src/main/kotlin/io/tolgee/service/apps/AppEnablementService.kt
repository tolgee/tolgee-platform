package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.dtos.apps.ProjectAppView
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import io.tolgee.repository.apps.AppInstallRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppEnablementService(
  private val appEnabledForProjectRepository: AppEnabledForProjectRepository,
  private val appInstallRepository: AppInstallRepository,
  private val appEnablementInserter: AppEnablementInserter,
  private val appAvailabilityService: AppAvailabilityService,
) {
  @Transactional
  fun enable(
    project: Project,
    installId: Long,
  ): AppInstall {
    val orgId = project.organizationOwner.id
    val install = resolveEnableableInstall(orgId, installId)

    if (appEnabledForProjectRepository.findByProjectIdAndAppInstallId(project.id, install.id) == null) {
      try {
        appEnablementInserter.insert(install.id, project.id)
      } catch (e: DataIntegrityViolationException) {
        // Only a concurrent enable is idempotent. The same exception also covers an FK violation
        // (install or project deleted mid-flight), which must not be reported as success.
        appEnabledForProjectRepository.findByProjectIdAndAppInstallId(project.id, install.id)
          ?: throw e
      }
    }

    return install
  }

  private fun resolveEnableableInstall(
    organizationId: Long,
    installId: Long,
  ): AppInstall {
    val install =
      appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    val app = install.app
    if (!appAvailabilityService.isAvailableForOrganization(app.organization.id, app.id, organizationId)) {
      throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    }
    return install
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
  fun listAppsForProject(
    project: Project,
    pageable: Pageable,
  ): Page<ProjectAppView> {
    return appInstallRepository.findProjectAppViews(project.id, project.organizationOwner.id, pageable)
  }

  @Transactional(readOnly = true)
  fun listEnabledInstallsForProject(projectId: Long): List<AppInstall> {
    return appEnabledForProjectRepository.findEnabledInstallsByProjectId(projectId)
  }

  @Transactional(readOnly = true)
  fun countEnabledProjectsForInstall(appInstallId: Long): Long {
    return countEnabledProjectsByInstall(listOf(appInstallId))[appInstallId] ?: 0
  }

  @Transactional(readOnly = true)
  fun countEnabledProjectsByInstall(appInstallIds: Collection<Long>): Map<Long, Long> {
    if (appInstallIds.isEmpty()) return emptyMap()
    return appEnabledForProjectRepository
      .countEnabledProjectsByInstallIds(appInstallIds)
      .associate { (it[0] as Long) to (it[1] as Long) }
  }

  @Transactional(readOnly = true)
  fun getEnabledProjectsForInstall(
    appInstallId: Long,
    pageable: Pageable,
  ): Page<Project> {
    return appEnabledForProjectRepository.findEnabledProjectsByAppInstallId(appInstallId, pageable)
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

  @Transactional
  fun removeAllForProject(projectId: Long) {
    appEnabledForProjectRepository.deleteByProjectId(projectId)
  }
}
