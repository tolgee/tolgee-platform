package io.tolgee.repository.oauth2

import io.tolgee.model.oauth2.OAuth2SupersededRefreshToken
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
interface OAuth2SupersededRefreshTokenRepository : JpaRepository<OAuth2SupersededRefreshToken, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM OAuth2SupersededRefreshToken s WHERE s.tokenHash = :hash")
  fun findAndLockByTokenHash(
    @Param("hash") hash: String,
  ): OAuth2SupersededRefreshToken?

  /**
   * Keeps the newest [keep] rotations of every grant, plus anything younger than [floor] whatever its rank.
   *
   * Age is the floor on **every** eviction, with no rank-only escape: rank is a function of how many rotations
   * followed a row, and a thief holding a stolen token produces those in minutes, so any branch that evicts by rank
   * alone is a lever for switching theft detection off. What bounds the table instead is the write side, which stops
   * recording once a grant is at its ceiling — see `OAuth2AuthorizationService.demoteToSupersededHistory`.
   */
  @Modifying
  @Query(
    value = """
      DELETE FROM oauth2_superseded_refresh_token t
      USING (
        SELECT id, row_number() OVER (
          PARTITION BY grant_id ORDER BY superseded_at DESC NULLS LAST, id DESC
        ) AS rn
        FROM oauth2_superseded_refresh_token
      ) ranked
      WHERE t.id = ranked.id
        AND ranked.rn > :keep
        AND (t.superseded_at IS NULL OR t.superseded_at < :floor)
    """,
    nativeQuery = true,
  )
  fun deleteBeyondNewestPerGrant(
    @Param("keep") keep: Int,
    @Param("floor") floor: Date,
  ): Int

  fun countByGrantId(grantId: Long): Long

  /**
   * Frees one slot on a grant at its ceiling, taking the oldest row already past [floor] - the row the prune
   * would take next. A query rather than the mapped collection, which would load every row of a grant that is at
   * its ceiling on a path that only ever writes one.
   */
  @Modifying
  @Query(
    value = """
      DELETE FROM oauth2_superseded_refresh_token t
      WHERE t.id = (
        SELECT id FROM oauth2_superseded_refresh_token
        WHERE grant_id = :grantId AND (superseded_at IS NULL OR superseded_at < :floor)
        ORDER BY superseded_at ASC NULLS FIRST, id ASC
        LIMIT 1
      )
    """,
    nativeQuery = true,
  )
  fun deleteOldestPastFloor(
    @Param("grantId") grantId: Long,
    @Param("floor") floor: Date,
  ): Int
}
