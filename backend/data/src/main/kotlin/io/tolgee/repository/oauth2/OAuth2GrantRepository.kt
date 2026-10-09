package io.tolgee.repository.oauth2

import io.tolgee.model.oauth2.OAuth2Grant
import jakarta.persistence.LockModeType
import org.springframework.context.annotation.Lazy
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

  /**
   * The client of the live grant this refresh token belongs to, without loading or locking the grant: the caller
   * locks and loads it afterwards, and a managed entity read here would keep the state it had before the document
   * check ran. It must take no lock, because the check writes to this row from its own transaction.
   */
  @Query(
    """SELECT g.clientId FROM OAuth2Grant g
       WHERE g.refreshTokenHash = :hash AND g.refreshTokenExpiresAt > :now""",
  )
  fun findLiveClientIdByRefreshTokenHash(
    @Param("hash") hash: String,
    @Param("now") now: Date,
  ): String?

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
   * How many different document-backed clients this user already holds a **live** grant for. A withdrawn client
   * drops out even while its mark could still be lifted: counting fewer is the safe direction for a cap.
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
