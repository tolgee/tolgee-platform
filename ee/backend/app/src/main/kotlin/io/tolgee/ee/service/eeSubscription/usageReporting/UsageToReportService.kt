package io.tolgee.ee.service.eeSubscription.usageReporting

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Caches
import io.tolgee.ee.data.usageReporting.UsageToReportDto
import io.tolgee.ee.model.UsageToReport
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import jakarta.persistence.EntityManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.Date

/**
 * Service for managing usage data that needs to be reported to Tolgee Cloud.
 *
 * This service is a key part of the deferred reporting mechanism. It stores
 * usage data locally when immediate reporting is deferred (due to the 1-minute rule),
 * and provides access to this data when it's time to send a report.
 */
@Service
@Suppress("SelfReferenceConstructorParameter")
class UsageToReportService(
  @Lazy
  private val self: UsageToReportService,
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
  private val keyService: KeyService,
  private val userAccountService: UserAccountService,
) {
  /**
   * Retrieves the current usage data and reporting status.
   *
   * This method provides access to both the current usage data that needs to be reported
   * and the last reported usage data. It's used by the deferral mechanism to determine
   * if a report should be sent immediately or deferred.
   *
   * The result is cached to improve performance, as this method is called frequently
   * during usage reporting operations.
   *
   * @return A DTO containing the current usage data and reporting status
   */
  @Cacheable(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  fun findOrCreateUsageToReport(): UsageToReportDto {
    return tryUntilItDoesntBreakConstraint {
      val dto = findDto()

      if (dto == null) {
        self.create()
      }

      findDto() ?: throw IllegalStateException("Usage to report should be present in database")
    }
  }

  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  @Transactional
  fun delete() {
    entityManager.createQuery("delete from UsageToReport lru").executeUpdate()
  }

  private fun findDto(): UsageToReportDto? =
    entityManager
      .createQuery(
        """
          |select 
          |new io.tolgee.ee.data.usageReporting.UsageToReportDto(
          |    lru.lastReportedKeys, 
          |     lru.lastReportedSeats, 
          |     lru.keysToReport, 
          |     lru.seatsToReport, 
          |     lru.reportedAt)
          |from UsageToReport lru
          |
        """.trimMargin(),
        UsageToReportDto::class.java,
      ).resultList
      .singleOrNull()

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected fun create() {
    val entity =
      UsageToReport().apply {
        keysToReport = keyService.countAllOnInstance()
        seatsToReport = userAccountService.countAllEnabled()
        // we can use this far in past distant date, because we haven't reported yes
        reportedAt = Date(1)
      }
    entityManager.persist(entity)
    entityManager.flush()
  }

  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  fun storeCurrentKeysUsage(keys: Long) {
    entityManager
      .createQuery(
        """
      update UsageToReport lru
      set lru.keysToReport = :keysToReport
      """,
      ).setParameter("keysToReport", keys)
      .executeUpdate()
  }

  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  fun storeCurrentSeatsUsage(seats: Long) {
    entityManager
      .createQuery(
        """
      update UsageToReport lru
      set lru.seatsToReport = :seatsToReport
      """,
      ).setParameter("seatsToReport", seats)
      .executeUpdate()
  }

  /**
   * Stores current usage data without reporting it immediately.
   *
   * This method is called when usage reporting is deferred due to the 1-minute rule.
   * It updates the local storage with the current usage data, which will be reported
   * later when the deferral period has passed.
   *
   * @param keys The current number of keys, or null if unchanged
   * @param seats The current number of seats, or null if unchanged
   */
  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  fun storeCurrentUsage(
    keys: Long?,
    seats: Long?,
  ) {
    if (keys != null) {
      storeCurrentKeysUsage(keys)
    }
    if (seats != null) {
      storeCurrentSeatsUsage(seats)
    }
  }

  /**
   * Updates the stored usage data after a report has been sent.
   *
   * This method is called when usage data is successfully reported to Tolgee Cloud.
   * It updates both the current usage data and the last reported usage data,
   * and records the current time as the report time. This timestamp is used by
   * the deferral mechanism to determine when the next report can be sent.
   *
   * @param keys The number of keys that were reported, or null if unchanged
   * @param seats The number of seats that were reported, or null if unchanged
   */
  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  fun storeOnReport(
    keys: Long?,
    seats: Long?,
  ) {
    if (keys != null) {
      storeOnReportKeys(keys)
    }
    if (seats != null) {
      storeOnReportSeats(seats)
    }
  }

  private fun storeOnReportKeys(keys: Long) {
    entityManager
      .createQuery(
        """
      update UsageToReport lru
      set lru.lastReportedKeys = :lastReportedKeys,
          lru.keysToReport = :keysToReport,
          lru.reportedAt = :reportedAt
      """,
      ).setParameter("lastReportedKeys", keys)
      .setParameter("keysToReport", keys)
      .setParameter("reportedAt", currentDateProvider.date)
      .executeUpdate()
  }

  private fun storeOnReportSeats(seats: Long) {
    entityManager
      .createQuery(
        """
      update UsageToReport lru  
      set lru.lastReportedSeats = :lastReportedSeats,
          lru.seatsToReport = :seatsToReport,
          lru.reportedAt = :reportedAt
      """,
      ).setParameter("lastReportedSeats", seats)
      .setParameter("seatsToReport", seats)
      .setParameter("reportedAt", currentDateProvider.date)
      .executeUpdate()
  }
}
