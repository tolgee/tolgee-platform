package io.tolgee.repository.oauth2

import io.tolgee.model.oauth2.OAuth2Authorization
import jakarta.persistence.LockModeType
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface OAuth2AuthorizationRepository : JpaRepository<OAuth2Authorization, Long> {
  fun findByConsentState(consentState: String): OAuth2Authorization?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM OAuth2Authorization a WHERE a.consentState = :consentState")
  fun findAndLockByConsentState(
    @Param("consentState") consentState: String,
  ): OAuth2Authorization?

  fun findByAccessTokenHash(accessTokenHash: String): OAuth2Authorization?

  fun countByUserAccountId(userAccountId: Long): Long

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM OAuth2Authorization a WHERE a.codeHash = :codeHash")
  fun findAndLockByCodeHash(
    @Param("codeHash") codeHash: String,
  ): OAuth2Authorization?

  /** Serializes rotation, so two concurrent refreshes cannot each believe they hold the grant's only token pair. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM OAuth2Authorization a WHERE a.refreshTokenHash = :hash")
  fun findAndLockByRefreshTokenHash(
    @Param("hash") hash: String,
  ): OAuth2Authorization?

  /** Reaches the grant a just-superseded token belonged to, which is what makes RFC 9700 §4.14.2 replay detectable. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM OAuth2Authorization a WHERE a.previousRefreshTokenHash = :hash")
  fun findAndLockByPreviousRefreshTokenHash(
    @Param("hash") hash: String,
  ): OAuth2Authorization?

  @Modifying
  @Query("DELETE FROM OAuth2Authorization a WHERE a.userAccount.id = :userAccountId")
  fun deleteAllByUserAccountId(userAccountId: Long): Int

  /** Removes grants no credential can revive: the newest expiry (refresh, then access, then code) is past the cutoff. */
  @Modifying
  @Query(
    """
    DELETE FROM OAuth2Authorization a
    WHERE COALESCE(a.refreshTokenExpiresAt, a.accessTokenExpiresAt, a.codeExpiresAt) < :cutoff
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
    DELETE FROM OAuth2Authorization a
    WHERE a.consentExpiresAt < :now
      AND a.codeHash IS NULL AND a.accessTokenHash IS NULL AND a.refreshTokenHash IS NULL
    """,
  )
  fun deleteExpiredPendingConsents(now: Date): Int
}
