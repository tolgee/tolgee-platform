package io.tolgee.repository.oauth2

import io.tolgee.model.oauth2.OAuth2Grant
import jakarta.persistence.LockModeType
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date

/**
 * The `findAndLock*` finders take a row lock: two concurrent redemptions of the same code, consent or refresh token
 * must not both observe it unspent and each walk away believing they hold the grant's only token pair.
 */
@Repository
@Lazy
interface OAuth2GrantRepository : JpaRepository<OAuth2Grant, Long> {
  fun findByConsentState(consentState: String): OAuth2Grant?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM OAuth2Grant g WHERE g.consentState = :consentState")
  fun findAndLockByConsentState(
    @Param("consentState") consentState: String,
  ): OAuth2Grant?

  fun findByAccessTokenHash(accessTokenHash: String): OAuth2Grant?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM OAuth2Grant g WHERE g.accessTokenHash = :accessTokenHash")
  fun findAndLockByAccessTokenHash(
    @Param("accessTokenHash") accessTokenHash: String,
  ): OAuth2Grant?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM OAuth2Grant g WHERE g.codeHash = :codeHash")
  fun findAndLockByCodeHash(
    @Param("codeHash") codeHash: String,
  ): OAuth2Grant?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM OAuth2Grant g WHERE g.refreshTokenHash = :hash")
  fun findAndLockByRefreshTokenHash(
    @Param("hash") hash: String,
  ): OAuth2Grant?

  /** Reaches the grant a just-superseded token belonged to, which is what makes RFC 9700 §4.14.2 replay detectable. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM OAuth2Grant g WHERE g.previousRefreshTokenHash = :hash")
  fun findAndLockByPreviousRefreshTokenHash(
    @Param("hash") hash: String,
  ): OAuth2Grant?

  @Modifying
  @Query("DELETE FROM OAuth2Grant g WHERE g.userAccount.id = :userAccountId")
  fun deleteAllByUserAccountId(userAccountId: Long): Int

  @Modifying
  @Query(
    """
    DELETE FROM OAuth2Grant g
    WHERE GREATEST(g.refreshTokenExpiresAt, g.accessTokenExpiresAt, g.codeExpiresAt) < :cutoff
    """,
  )
  fun deleteExpiredBefore(cutoff: Date): Int

  /** A consent the user never completed holds no code and no tokens, so it skips the replay-evidence retention. */
  @Modifying
  @Query(
    """
    DELETE FROM OAuth2Grant g
    WHERE g.consentExpiresAt < :now
      AND g.codeHash IS NULL AND g.accessTokenHash IS NULL AND g.refreshTokenHash IS NULL
    """,
  )
  fun deleteExpiredPendingConsents(now: Date): Int

  /**
   * How many different document-backed clients this user already holds a **live** grant for.
   *
   * This must never count more than [findCimdClientIdsDueForCheck] would return: a client counted but not queued
   * is a slot taken from the user for a row that costs the queue nothing. Counting fewer is safe, so both tests
   * here are the blunt ones - a withdrawn client drops out even while its mark could still be lifted.
   */
  @Query(
    """
    SELECT COUNT(DISTINCT g.clientId) FROM OAuth2Grant g
    WHERE g.userAccount.id = :userId AND g.clientId LIKE 'http%' AND g.clientId <> :exceptClientId
      AND g.clientWithdrawnAt IS NULL
      AND GREATEST(g.refreshTokenExpiresAt, g.accessTokenExpiresAt, g.codeExpiresAt) > :now
    """,
  )
  fun countDocumentBackedClientsOfUser(
    @Param("userId") userId: Long,
    @Param("exceptClientId") exceptClientId: String,
    @Param("now") now: Date,
  ): Long

  /**
   * Records that this client's document was read and accepted, for every grant of it. Only rows whose stamp is
   * already older than [refreshBefore] are written, so a client read every few minutes does not rewrite them all.
   */
  @Modifying
  @Query(
    """UPDATE OAuth2Grant g SET g.cimdVerifiedAt = :at
       WHERE g.clientId = :clientId AND (g.cimdVerifiedAt IS NULL OR g.cimdVerifiedAt < :refreshBefore)""",
  )
  fun markClientDocumentRead(
    @Param("clientId") clientId: String,
    @Param("at") at: Date,
    @Param("refreshBefore") refreshBefore: Date,
  ): Int

  /**
   * Revokes every grant of this client whose consented terms the document no longer matches.
   *
   * [consentedScheme] keeps out hashes this build cannot reproduce - reading one of those as drift would revoke
   * every grant an earlier release issued.
   */
  @Modifying
  @Query(
    """DELETE FROM OAuth2Grant g
       WHERE g.clientId = :clientId
         AND g.clientMetadataHash LIKE :consentedScheme
         AND g.clientMetadataHash <> :currentHash""",
  )
  fun deleteDriftedFromDocument(
    @Param("clientId") clientId: String,
    @Param("currentHash") currentHash: String,
    @Param("consentedScheme") consentedScheme: String,
  ): Int

  /**
   * One round's worth of `client_id` URLs to read, chosen and ordered by the database: URL-shaped, with at least
   * one grant that is still usable, not already retired for good, not read within the interval, least recently
   * attempted first, and no more than [pageable] of them.
   *
   * "Retired for good" is measured with `previousCheckedAt` and not with a clock. A mark newer than that attempt,
   * or newer than [markedAfter], keeps the client in the list - see
   * `OAuth2AuthorizationService.clearClientWithdrawn`, which picks the attempt.
   */
  @Query(
    """
    SELECT g.clientId FROM OAuth2Grant g
    LEFT JOIN OAuth2ClientDocumentCheck c ON c.clientId = g.clientId
    WHERE g.clientId LIKE 'http%'
      AND GREATEST(g.refreshTokenExpiresAt, g.accessTokenExpiresAt, g.codeExpiresAt) > :now
    GROUP BY g.clientId
    HAVING (MAX(c.checkedAt) IS NULL OR MAX(c.checkedAt) < :dueBefore)
      AND (
        COUNT(g.clientWithdrawnAt) < COUNT(g.id)
        OR MAX(g.clientWithdrawnAt) > :markedAfter
        OR MAX(g.clientWithdrawnAt) > COALESCE(MAX(c.previousCheckedAt), :never)
      )
    ORDER BY COALESCE(MAX(c.checkedAt), MIN(g.createdAt)) ASC, g.clientId ASC
    """,
  )
  fun findCimdClientIdsDueForCheck(
    @Param("now") now: Date,
    @Param("dueBefore") dueBefore: Date,
    @Param("markedAfter") markedAfter: Date,
    @Param("never") never: Date,
    pageable: Pageable,
  ): List<String>

  /** How many the query above would return without its limit, for the gauge that says whether the job keeps up. */
  @Query(
    value = """
      SELECT COUNT(*) FROM (
        SELECT g.client_id FROM oauth2_grant g
        LEFT JOIN oauth2_client_document_check c ON c.client_id = g.client_id
        WHERE g.client_id LIKE 'http%'
          AND GREATEST(g.refresh_token_expires_at, g.access_token_expires_at, g.code_expires_at) > :now
        GROUP BY g.client_id
        HAVING (MAX(c.checked_at) IS NULL OR MAX(c.checked_at) < :dueBefore)
          AND (
            COUNT(g.client_withdrawn_at) < COUNT(g.id)
            OR MAX(g.client_withdrawn_at) > :markedAfter
            OR MAX(g.client_withdrawn_at) > COALESCE(MAX(c.previous_checked_at), :never)
          )
      ) due
    """,
    nativeQuery = true,
  )
  fun countCimdClientIdsDueForCheck(
    @Param("now") now: Date,
    @Param("dueBefore") dueBefore: Date,
    @Param("markedAfter") markedAfter: Date,
    @Param("never") never: Date,
  ): Long

  /**
   * Records a publisher's withdrawal where it survives the process that saw it and reaches every other instance.
   * The grants are left in place rather than deleted, so a mis-deploy can still be recovered from.
   */
  @Modifying
  @Query(
    """UPDATE OAuth2Grant g SET g.clientWithdrawnAt = :at WHERE g.clientId = :clientId AND g.clientWithdrawnAt IS NULL""",
  )
  fun markClientWithdrawn(
    @Param("clientId") clientId: String,
    @Param("at") at: Date,
  ): Int

  /**
   * Lifts a withdrawal that is still young enough to be a mis-deploy, by either of two measures: it was made
   * within [markedAfter], or it is newer than [previousAttempt]. A mark past both stays.
   */
  @Modifying
  @Query(
    """UPDATE OAuth2Grant g SET g.clientWithdrawnAt = null
       WHERE g.clientId = :clientId
         AND (g.clientWithdrawnAt > :markedAfter OR g.clientWithdrawnAt > :previousAttempt)""",
  )
  fun clearRecentClientWithdrawn(
    @Param("clientId") clientId: String,
    @Param("markedAfter") markedAfter: Date,
    @Param("previousAttempt") previousAttempt: Date,
  ): Int
}
