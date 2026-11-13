package io.tolgee.repository.oauth2

import io.tolgee.model.oauth2.OAuth2ClientDocumentCheck
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface OAuth2ClientDocumentCheckRepository : JpaRepository<OAuth2ClientDocumentCheck, Long> {
  fun findByClientId(clientId: String): OAuth2ClientDocumentCheck?

  fun existsByClientId(clientId: String): Boolean

  /**
   * Takes the next attempt for this client if the last one is older than [dueBefore]. One statement, so two callers
   * racing for the same client cannot both win: the second one re-reads the row the first one wrote and matches
   * nothing.
   */
  @Modifying
  @Query(
    """
    UPDATE OAuth2ClientDocumentCheck c
    SET c.previousCheckedAt = c.checkedAt, c.checkedAt = :now
    WHERE c.clientId = :clientId AND c.checkedAt < :dueBefore
    """,
  )
  fun claimAttempt(
    @Param("clientId") clientId: String,
    @Param("now") now: Date,
    @Param("dueBefore") dueBefore: Date,
  ): Int

  /** Undoes [claimAttempt] for a fetch that never happened, so the client is due again at once. */
  @Modifying
  @Query(
    """
    UPDATE OAuth2ClientDocumentCheck c
    SET c.checkedAt = c.previousCheckedAt, c.previousCheckedAt = null
    WHERE c.clientId = :clientId AND c.checkedAt = :claimedAt AND c.previousCheckedAt IS NOT NULL
    """,
  )
  fun releaseAttempt(
    @Param("clientId") clientId: String,
    @Param("claimedAt") claimedAt: Date,
  ): Int

  /** The first-ever claim has nothing to fall back to, so releasing it removes the row. */
  @Modifying
  @Query(
    """
    DELETE FROM OAuth2ClientDocumentCheck c
    WHERE c.clientId = :clientId AND c.checkedAt = :claimedAt AND c.previousCheckedAt IS NULL
    """,
  )
  fun deleteFirstAttempt(
    @Param("clientId") clientId: String,
    @Param("claimedAt") claimedAt: Date,
  ): Int

  /** A client nobody holds a grant for any more is never checked again, so its row is dead weight. */
  @Modifying
  @Query(
    """
    DELETE FROM OAuth2ClientDocumentCheck c
    WHERE NOT EXISTS (SELECT 1 FROM OAuth2Grant g WHERE g.clientId = c.clientId)
    """,
  )
  fun deleteWithoutGrants(): Int
}
