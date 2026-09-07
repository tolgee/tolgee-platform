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
          |     lru.lastReportedWords,
          |     lru.keysToReport,
          |     lru.seatsToReport,
          |     lru.wordsToReport,
          |     lru.wordsDirty,
          |     lru.wordsCountedAt,
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
        // Not counted here: on a word-metered instance the flag makes the first periodic report
        // take the count, and on any other instance nothing pays for an aggregation it cannot use.
        wordsDirty = true
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
   * @param words The current number of words, or null if unchanged
   */
  @Transactional
  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  fun storeCurrentUsage(
    keys: Long? = null,
    seats: Long? = null,
    words: Long? = null,
  ) {
    if (keys != null) {
      storeCurrentKeysUsage(keys)
    }
    if (seats != null) {
      storeCurrentSeatsUsage(seats)
    }
    if (words != null) {
      storeCurrentWordsUsage(words)
    }
  }

  private fun storeCurrentWordsUsage(words: Long) {
    entityManager
      .createQuery(
        """
      update UsageToReport lru
      set lru.wordsToReport = :wordsToReport,
          lru.wordsCountedAt = :countedAt
      """,
      ).setParameter("wordsToReport", words)
      .setParameter("countedAt", currentDateProvider.date)
      .executeUpdate()
  }

  // Unconditional on purpose: the row lock is what makes takeWordsDirty wait for a writer that has
  // raised the flag and not yet committed. The price is that it is taken before the activity
  // revision is written and held to commit, so concurrent translation commits on a word-metered
  // instance serialise behind the largest in-flight write's activity storage.
  @Transactional
  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  fun markWordsDirty() {
    entityManager
      .createQuery(
        """
      update UsageToReport lru
      set lru.wordsDirty = true
      """,
      ).executeUpdate()
  }

  /**
   * Reads and clears the flag in one statement, so the answer comes from the row rather than from
   * the cached DTO — an eviction that lands before the raising writer commits would otherwise let a
   * concurrent read repopulate the cache with a stale `false` that nothing evicts again.
   *
   * Its own transaction, so the row lock is not held across the caller's word count. That also
   * means a caller whose own transaction already touched this row would block on itself — only the
   * reporting path may call it.
   *
   * The eviction condition keeps an idle word-metered instance from evicting the cache on every
   * tick: the recount window stays open until something is actually counted, so this otherwise runs
   * once a minute forever and clears nothing. `#result` requires beforeInvocation to stay false.
   *
   * @return whether the flag was raised, and therefore whether a recount is owed
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1", condition = "#result")
  fun takeWordsDirty(): Boolean =
    entityManager
      .createQuery(
        """
      update UsageToReport lru
      set lru.wordsDirty = false
      where lru.wordsDirty = true
      """,
      ).executeUpdate() > 0

  /** For the reporting path's own failure recovery, which cannot use the writer-path variant. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  fun markWordsDirtyInNewTransaction() {
    markWordsDirty()
  }

  /**
   * Its own transaction: the caller counts words outside any lock, so this must not extend the
   * reporting transaction's hold on the row across the licence-server call that follows.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  fun storeRecountedWords(words: Long) {
    storeCurrentWordsUsage(words)
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
   * @param words The number of words that were reported, or null if unchanged
   */
  @Transactional
  @CacheEvict(Caches.EE_LAST_REPORTED_USAGE, key = "1")
  fun storeOnReport(
    keys: Long?,
    seats: Long?,
    words: Long? = null,
  ) {
    if (keys != null) {
      storeOnReportKeys(keys)
    }
    if (seats != null) {
      storeOnReportSeats(seats)
    }
    if (words != null) {
      storeOnReportWords(words)
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

  private fun storeOnReportWords(words: Long) {
    entityManager
      .createQuery(
        """
      update UsageToReport lru
      set lru.lastReportedWords = :lastReportedWords,
          lru.wordsToReport = :wordsToReport
      """,
      ).setParameter("lastReportedWords", words)
      .setParameter("wordsToReport", words)
      .executeUpdate()
  }
}
