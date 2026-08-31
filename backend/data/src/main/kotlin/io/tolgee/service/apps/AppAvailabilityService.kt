package io.tolgee.service.apps

import io.tolgee.model.Organization
import io.tolgee.repository.apps.AppAvailabilityRepository
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides which organizations may install an app they do not own. Availability is a set of rows on
 * [io.tolgee.model.apps.AppAvailability]: a row with a real organization makes the app installable by
 * that organization, and the single null-organization sentinel row makes it installable by every
 * organization. The owner is always available and is never stored here.
 */
@Service
class AppAvailabilityService(
  private val appAvailabilityRepository: AppAvailabilityRepository,
  private val appEnabledForProjectRepository: AppEnabledForProjectRepository,
  private val appEnablementCache: AppEnablementCache,
) {
  /** Server-admin action: offer the app to every organization. Idempotent. */
  @Transactional
  fun setAvailableToAll(appEntityId: Long) {
    appAvailabilityRepository.insertIfAbsent(appEntityId, organizationId = null)
  }

  /** Server-admin action: withdraw the blanket offer. */
  @Transactional
  fun clearAvailableToAll(appEntityId: Long) {
    val sentinel = appAvailabilityRepository.findByAppIdAndOrganizationIsNull(appEntityId) ?: return
    appAvailabilityRepository.delete(sentinel)
    appAvailabilityRepository.flush()
    appEnabledForProjectRepository.disableWhereNoLongerAvailable(appEntityId)
    // The bulk disable removed enablements for still-live installs across organizations; which ones is
    // not known here, and withdrawing availability is a rare admin action, so evict every entry.
    appEnablementCache.evictAll()
  }

  /** Server-admin action: make the app installable by one organization. Idempotent. */
  @Transactional
  fun addAvailableOrganization(
    appEntityId: Long,
    organizationId: Long,
  ) {
    appAvailabilityRepository.insertIfAbsent(appEntityId, organizationId)
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
    // See clearAvailableToAll: the bulk disable touched an unknown set of installs, so evict every entry.
    appEnablementCache.evictAll()
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
}
