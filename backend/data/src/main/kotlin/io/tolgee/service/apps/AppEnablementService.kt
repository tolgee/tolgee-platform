package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.AppEnabledForProject
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import io.tolgee.repository.apps.AppInstallRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppEnablementService(
  private val appEnabledForProjectRepository: AppEnabledForProjectRepository,
  private val appInstallRepository: AppInstallRepository,
  private val appManagedAutomationService: AppManagedAutomationService,
) {
  @Transactional
  fun enable(
    project: Project,
    installId: Long,
    author: UserAccount,
  ): AppInstall {
    val orgId = project.organizationOwner.id
    val install =
      appInstallRepository.findByOrganizationIdAndId(orgId, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)

    appEnabledForProjectRepository
      .findByProjectIdAndAppInstallId(project.id, install.id)
      ?.let { return install }

    appEnabledForProjectRepository.save(
      AppEnabledForProject().apply {
        this.appInstall = install
        this.project = project
        this.author = author
      },
    )
    appManagedAutomationService.onEnable(install, project)
    return install
  }

  @Transactional
  fun disable(
    projectId: Long,
    appInstallId: Long,
  ) {
    val existing =
      appEnabledForProjectRepository.findByProjectIdAndAppInstallId(projectId, appInstallId) ?: return
    appManagedAutomationService.onDisable(existing.appInstall, existing.project)
    appEnabledForProjectRepository.delete(existing)
  }

  @Transactional(readOnly = true)
  fun listAppsForProject(project: Project): List<Pair<AppInstall, Boolean>> {
    val installs = appInstallRepository.findAllByOrganizationId(project.organizationOwner.id)
    val enabledIds =
      appEnabledForProjectRepository
        .findAllByProjectId(project.id)
        .map { it.appInstall.id }
        .toSet()
    return installs.map { it to (it.id in enabledIds) }
  }

  @Transactional(readOnly = true)
  fun listEnabledAppsForProject(project: Project): List<AppInstall> {
    return appEnabledForProjectRepository
      .findAllByProjectId(project.id)
      .map { it.appInstall }
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
    appEnabledForProjectRepository
      .findAllByAppInstallId(appInstallId)
      .forEach { enablement ->
        appManagedAutomationService.onDisable(enablement.appInstall, enablement.project)
        appEnabledForProjectRepository.delete(enablement)
      }
  }
}
