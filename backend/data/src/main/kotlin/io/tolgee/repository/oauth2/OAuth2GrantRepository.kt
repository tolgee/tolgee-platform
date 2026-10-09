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
   * The client of the live grant this refresh token belongs to. It loads no entity and takes no lock. The document
   * check writes to this grant's row from its own transaction, and the refresh locks and loads the row afterwards.
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
   * How many different document-backed clients this user already holds a **live** grant for. A grant is
   * document-backed when it carries the hash of the document it was consented against, which
   * [io.tolgee.security.oauth2.OAuth2AuthorizationService.startAuthorization] writes for every client that came
   * through the CIMD path and for no other. A withdrawn
   * client drops out even while its mark could still be lifted: counting fewer is the safe direction for a cap.
   */
  @Query(
    """
    SELECT COUNT(DISTINCT g.clientId) FROM OAuth2Grant g
    WHERE g.userAccount.id = :userId AND g.clientMetadataHash IS NOT NULL AND g.clientId <> :exceptClientId
      AND NOT EXISTS (
        SELECT 1 FROM OAuth2ClientDocumentCheck c WHERE c.clientId = g.clientId AND c.withdrawnAt IS NOT NULL
      )
      AND GREATEST(g.refreshTokenExpiresAt, g.accessTokenExpiresAt, g.codeExpiresAt) > :now
    """,
  )
  fun countDocumentBackedClientsOfUser(
    @Param("userId") userId: Long,
    @Param("exceptClientId") exceptClientId: String,
    @Param("now") now: Date,
  ): Long

  /**
   * Revokes every grant of this client whose consented terms the document no longer matches: its stored hash differs
   * from [currentHash].
   *
   * Only hashes matching [reproducibleHashPattern] (a `LIKE` pattern such as `v1:%`) are compared. A hash written
   * by an older scheme cannot be recomputed by this build, so a mismatch there proves nothing, and reading it as
   * drift would revoke every grant an earlier release issued.
   */
  @Modifying
  @Query(
    """DELETE FROM OAuth2Grant g
       WHERE g.clientId = :clientId
         AND g.clientMetadataHash LIKE :reproducibleHashPattern
         AND g.clientMetadataHash <> :currentHash""",
  )
  fun deleteDriftedFromDocument(
    @Param("clientId") clientId: String,
    @Param("currentHash") currentHash: String,
    @Param("reproducibleHashPattern") reproducibleHashPattern: String,
  ): Int
}
