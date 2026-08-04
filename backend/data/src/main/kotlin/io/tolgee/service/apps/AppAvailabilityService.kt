package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.AppAvailableForOrganization
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppAvailableForOrganizationRepository
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import io.tolgee.repository.apps.AppInstallRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides which organizations may use a native (server-level) app install. A project can only enable
 * a native app once its organization has been granted availability here.
 */
@Service
class AppAvailabilityService(
  private val appAvailableForOrganizationRepository: AppAvailableForOrganizationRepository,
  private val appEnabledForProjectRepository: AppEnabledForProjectRepository,
  private val appInstallRepository: AppInstallRepository,
  private val entityManager: EntityManager,
) {
  @Transactional
  fun grant(
    installId: Long,
    organizationId: Long,
    author: UserAccount,
  ): AppAvailableForOrganization {
    val existing = appAvailableForOrganizationRepository.findByAppInstallIdAndOrganizationId(installId, organizationId)
    if (existing != null) return existing

    val install =
      appInstallRepository.findByOrganizationIsNullAndId(installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    val organization =
      entityManager.find(Organization::class.java, organizationId)
        ?: throw NotFoundException(Message.ORGANIZATION_NOT_FOUND)

    return appAvailableForOrganizationRepository.save(
      AppAvailableForOrganization().apply {
        this.appInstall = install
        this.organization = organization
        this.author = author
      },
    )
  }

  /**
   * Also disables the app in every project of the organization. Without that, an app whose
   * availability was revoked would keep running in the projects that had already enabled it.
   */
  @Transactional
  fun revoke(
    installId: Long,
    organizationId: Long,
  ) {
    val native = appInstallRepository.findByOrganizationIsNullAndId(installId) ?: return
    appEnabledForProjectRepository.deleteByAppInstallIdAndProjectOrganizationOwnerId(native.id, organizationId)
    val existing =
      appAvailableForOrganizationRepository.findByAppInstallIdAndOrganizationId(installId, organizationId) ?: return
    appAvailableForOrganizationRepository.delete(existing)
  }

  @Transactional(readOnly = true)
  fun listOrganizations(installId: Long): List<Organization> {
    return appAvailableForOrganizationRepository.findOrganizationsByAppInstallId(installId)
  }

  @Transactional(readOnly = true)
  fun listNativeInstallsForOrganization(organizationId: Long): List<AppInstall> {
    return appAvailableForOrganizationRepository.findNativeInstallsByOrganizationId(organizationId)
  }

  @Transactional(readOnly = true)
  fun isAvailableForOrganization(
    organizationId: Long,
    installId: Long,
  ): Boolean {
    return appAvailableForOrganizationRepository.findByAppInstallIdAndOrganizationId(installId, organizationId) != null
  }

  @Transactional
  fun removeAllForAppInstall(installId: Long) {
    appAvailableForOrganizationRepository.deleteByAppInstallId(installId)
  }
}
