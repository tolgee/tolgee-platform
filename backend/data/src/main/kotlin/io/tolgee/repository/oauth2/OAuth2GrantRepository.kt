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

  /**
   * A consent the user never completed holds no code and no tokens, so nothing can revive it once its own short
   * deadline passes — it does not wait for the retention window that exists to keep a spent code's replay evidence.
   */
  @Modifying
  @Query(
    """
    DELETE FROM OAuth2Grant g
    WHERE g.consentExpiresAt < :now
      AND g.codeHash IS NULL AND g.accessTokenHash IS NULL AND g.refreshTokenHash IS NULL
    """,
  )
  fun deleteExpiredPendingConsents(now: Date): Int
}
