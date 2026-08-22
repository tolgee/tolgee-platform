package io.tolgee.service.apps

import io.tolgee.model.Organization
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppAvailability
import io.tolgee.repository.apps.AppAvailabilityRepository
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import jakarta.persistence.EntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
  /** Server-admin action: offer the app to every organization. Idempotent. */
  @Transactional
  fun setAvailableToAll(appEntityId: Long) {
    if (appAvailabilityRepository.existsByAppIdAndOrganizationIsNull(appEntityId)) return
    addRow(appEntityId, organizationId = null)
  }

  /** Server-admin action: withdraw the blanket offer. */
  @Transactional
  fun clearAvailableToAll(appEntityId: Long) {
    val sentinel = appAvailabilityRepository.findByAppIdAndOrganizationIsNull(appEntityId) ?: return
    appAvailabilityRepository.delete(sentinel)
    appAvailabilityRepository.flush()
    appEnabledForProjectRepository.disableWhereNoLongerAvailable(appEntityId)
  }

  /** Server-admin action: make the app installable by one organization. Idempotent. */
  @Transactional
  fun addAvailableOrganization(
    appEntityId: Long,
    organizationId: Long,
  ) {
    if (appAvailabilityRepository.existsByAppIdAndOrganizationId(appEntityId, organizationId)) return
    addRow(appEntityId, organizationId)
  }

  /** Server-admin action: withdraw one organization's grant. */
  @Transactional
  fun removeAvailableOrganization(
    appEntityId: Long,
    organizationId: Long,
  ) {
    val existing =
      appAvailabilityRepository.findByAppIdAndOrganizationId(appEntityId, organizationId) ?: return
    appAvailabilityRepository.delete(existing)
    appAvailabilityRepository.flush()
    appEnabledForProjectRepository.disableWhereNoLongerAvailable(appEntityId)
  }

  /** Whether the app carries the all-organizations sentinel. */
  @Transactional(readOnly = true)
  fun isAvailableToAll(appEntityId: Long): Boolean {
    return appAvailabilityRepository.existsByAppIdAndOrganizationIsNull(appEntityId)
  }

  /** Which of these apps carry the all-organizations sentinel, in one query. */
  @Transactional(readOnly = true)
  fun availableToAllApps(appEntityIds: Collection<Long>): Set<Long> {
    if (appEntityIds.isEmpty()) return emptySet()
    return appAvailabilityRepository.findAppIdsAvailableToAll(appEntityIds).toSet()
  }

  /** The organizations the app is specifically offered to, besides the owner, paged for the admin view. */
  @Transactional(readOnly = true)
  fun findAvailableOrganizations(
    appEntityId: Long,
    search: String?,
    pageable: Pageable,
  ): Page<Organization> {
    return appAvailabilityRepository.findAvailableOrganizations(appEntityId, search?.ifBlank { null }, pageable)
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

  /**
   * Inserts an availability row, treating the unique-constraint violation a concurrent grant of the
   * same target raises as success - the row it would have added is already there.
   */
  private fun addRow(
    appEntityId: Long,
    organizationId: Long?,
  ) {
    val row =
      AppAvailability().apply {
        app = entityManager.getReference(App::class.java, appEntityId)
        organization = organizationId?.let { entityManager.getReference(Organization::class.java, it) }
      }
    try {
      appAvailabilityRepository.saveAndFlush(row)
    } catch (_: DataIntegrityViolationException) {
      // A concurrent grant of the same target won the unique-constraint race; the grant already holds.
    }
  }
}
