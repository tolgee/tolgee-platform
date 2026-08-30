package io.tolgee.service.apps

import io.tolgee.Metrics
import io.tolgee.activity.data.ActivityType
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
  private val appActivityRecorder: AppActivityRecorder,
  private val appEnablementCache: AppEnablementCache,
  private val metrics: Metrics,
) {
  @Transactional
  fun enable(
    project: Project,
    installId: Long,
  ): AppInstall {
    val orgId = project.organizationOwner.id
    val install = resolveEnableableInstall(orgId, installId)
    appActivityRecorder.record(install.app, ActivityType.APP_ENABLE_FOR_PROJECT, projectId = project.id)
    metrics.recordAppEnabledForProject()

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

    appEnablementCache.evict(install.id)
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
    appActivityRecorder.record(existing.appInstall.app, ActivityType.APP_DISABLE_FOR_PROJECT, projectId = projectId)
    appEnabledForProjectRepository.delete(existing)
    appEnablementCache.evict(appInstallId)
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

  fun isEnabledForProject(
    projectId: Long,
    appInstallId: Long,
  ): Boolean {
    return projectId in appEnablementCache.getEnabledProjectIds(appInstallId)
  }

  @Transactional
  fun removeAllForAppInstall(appInstallId: Long) {
    appEnabledForProjectRepository.deleteByAppInstallId(appInstallId)
    appEnablementCache.evict(appInstallId)
  }

  @Transactional
  fun removeAllForProject(projectId: Long) {
    appEnabledForProjectRepository.deleteByProjectId(projectId)
    // A project delete touches many installs' enabled-project sets; per-install eviction would need an
    // extra query and project deletion is rare, so evict every entry.
    appEnablementCache.evictAll()
  }
}
