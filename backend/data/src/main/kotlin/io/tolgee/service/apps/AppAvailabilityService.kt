package io.tolgee.service.apps

import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import io.tolgee.repository.apps.AppInstallRepository
import io.tolgee.repository.apps.AppRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides which organizations may use an app they do not own. An app is either private to its owner
 * or, when a server admin flips [io.tolgee.model.apps.App.availableToAllOrganizations], available to
 * every organization on the server. There is no per-organization grant list anymore — the flag is
 * all or the owner.
 */
@Service
class AppAvailabilityService(
  private val appRepository: AppRepository,
  private val appInstallRepository: AppInstallRepository,
  private val appEnabledForProjectRepository: AppEnabledForProjectRepository,
) {
  /** Server-admin action: offer the app to every organization, or withdraw it to the owner only. */
  @Transactional
  fun setAvailableToAllOrganizations(
    appEntityId: Long,
    available: Boolean,
  ) {
    val app = appRepository.findById(appEntityId).orElse(null) ?: return
    if (app.availableToAllOrganizations == available) return
    app.availableToAllOrganizations = available
    appRepository.save(app)

    // Withdrawing availability must not leave the app running in projects that could only reach it
    // through the blanket offer — every project whose organization does not own the app.
    if (!available) {
      appEnabledForProjectRepository.deleteByAppIdAndProjectOrganizationNotOwner(app.id)
    }
  }

  /** An organization may use [install]'s app if it owns it or the app is offered to everyone. */
  fun isAvailableForOrganization(
    organizationId: Long,
    install: AppInstall,
  ): Boolean {
    if (install.organization.id == organizationId) return true
    return install.app.availableToAllOrganizations
  }

  /**
   * Installs of server-wide apps this organization does not own — the ones its projects may enable
   * on top of the organization's own installs.
   */
  @Transactional(readOnly = true)
  fun listAvailableInstallsForOrganization(organizationId: Long): List<AppInstall> {
    return appInstallRepository.findAvailableInstallsForOrganization(organizationId)
  }

  @Transactional
  fun removeAllForAppInstall(appInstallId: Long) {
    // Availability is a property of the app, not the install, so there is nothing per-install to
    // remove — kept for call-site symmetry with the enablement cleanup.
  }
}
