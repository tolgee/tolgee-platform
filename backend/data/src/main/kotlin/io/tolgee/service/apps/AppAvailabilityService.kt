package io.tolgee.service.apps

import io.tolgee.model.Organization
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppAvailability
import io.tolgee.repository.apps.AppAvailabilityRepository
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides which organizations may install an app they do not own. Availability is a set of rows on
 * [AppAvailability]: a row with a real organization makes the app installable by that organization,
 * and the single null-organization sentinel row makes it installable by every organization. The
 * owner is always available and is never stored here.
 */
@Service
class AppAvailabilityService(
  private val appAvailabilityRepository: AppAvailabilityRepository,
  private val appEnabledForProjectRepository: AppEnabledForProjectRepository,
  private val entityManager: EntityManager,
) {
  data class Availability(
    val availableToAll: Boolean,
    val organizations: List<Organization>,
  )

  /** Server-admin action: offer the app to every organization, or withdraw the blanket offer. */
  @Transactional
  fun setAvailableToAll(
    appEntityId: Long,
    available: Boolean,
  ) {
    val sentinel = appAvailabilityRepository.findByAppIdAndOrganizationIsNull(appEntityId)
    if (available) {
      if (sentinel != null) return
      addRow(appEntityId, organizationId = null)
      return
    }
    if (sentinel == null) return
    appAvailabilityRepository.delete(sentinel)
    appAvailabilityRepository.flush()
    appEnabledForProjectRepository.disableWhereNoLongerAvailable(appEntityId)
  }

  /** Server-admin action: make the app installable by one organization, or withdraw that grant. */
  @Transactional
  fun setAvailableToOrganization(
    appEntityId: Long,
    organizationId: Long,
    available: Boolean,
  ) {
    val existing = appAvailabilityRepository.findByAppIdAndOrganizationId(appEntityId, organizationId)
    if (available) {
      if (existing != null) return
      addRow(appEntityId, organizationId)
      return
    }
    if (existing == null) return
    appAvailabilityRepository.delete(existing)
    appAvailabilityRepository.flush()
    appEnabledForProjectRepository.disableWhereNoLongerAvailable(appEntityId)
  }

  /** The app's whole availability set: the blanket flag and the specific organizations. */
  @Transactional(readOnly = true)
  fun listAvailability(appEntityId: Long): Availability {
    val availableToAll = appAvailabilityRepository.existsByAppIdAndOrganizationIsNull(appEntityId)
    val organizations =
      appAvailabilityRepository
        .findByAppIdAndOrganizationIsNotNullOrderByOrganizationNameAsc(appEntityId)
        .mapNotNull { it.organization }
    return Availability(availableToAll = availableToAll, organizations = organizations)
  }

  /**
   * Whether [organizationId] may install (and keep enabling) the app: it owns it, the app is offered
   * to everyone, or the app is offered to this organization specifically. Read fresh every time so a
   * concurrently-withdrawn availability is respected at once.
   */
  @Transactional(readOnly = true)
  fun isAvailableForOrganization(
    ownerOrganizationId: Long,
    appEntityId: Long,
    organizationId: Long,
  ): Boolean {
    if (ownerOrganizationId == organizationId) return true
    if (appAvailabilityRepository.existsByAppIdAndOrganizationIsNull(appEntityId)) return true
    return appAvailabilityRepository.existsByAppIdAndOrganizationId(appEntityId, organizationId)
  }

  @Transactional
  fun removeAllForApp(appEntityId: Long) {
    appAvailabilityRepository.deleteByAppId(appEntityId)
  }

  private fun addRow(
    appEntityId: Long,
    organizationId: Long?,
  ) {
    val row =
      AppAvailability().apply {
        app = entityManager.getReference(App::class.java, appEntityId)
        organization = organizationId?.let { entityManager.getReference(Organization::class.java, it) }
      }
    appAvailabilityRepository.save(row)
  }
}
