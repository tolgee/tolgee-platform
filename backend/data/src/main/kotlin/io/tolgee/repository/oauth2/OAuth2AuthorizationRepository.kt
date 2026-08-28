package io.tolgee.repository.oauth2

import io.tolgee.model.oauth2.OAuth2Authorization
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface OAuth2AuthorizationRepository : JpaRepository<OAuth2Authorization, Long> {
  fun findByConsentState(consentState: String): OAuth2Authorization?

  fun findByCodeHash(codeHash: String): OAuth2Authorization?

  fun findByAccessTokenHash(accessTokenHash: String): OAuth2Authorization?

  fun findByRefreshTokenHash(refreshTokenHash: String): OAuth2Authorization?

  fun countByUserAccountId(userAccountId: Long): Long

  @Modifying
  @Query("DELETE FROM OAuth2Authorization a WHERE a.userAccount.id = :userAccountId")
  fun deleteAllByUserAccountId(userAccountId: Long): Int

  /**
   * Removes authorizations no credential can revive: the newest expiry (refresh, then access, then code) is past the
   * cutoff, or nothing was ever issued and the row itself is older than the cutoff (an abandoned consent).
   */
  @Modifying
  @Query(
    """
    DELETE FROM OAuth2Authorization a
    WHERE COALESCE(a.refreshTokenExpiresAt, a.accessTokenExpiresAt, a.codeExpiresAt) < :cutoff
       OR (a.refreshTokenExpiresAt IS NULL AND a.accessTokenExpiresAt IS NULL AND a.codeExpiresAt IS NULL
           AND a.createdAt < :cutoff)
    """,
  )
  fun deleteExpiredBefore(cutoff: Date): Int
}
