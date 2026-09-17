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
   * Keeps the newest [keep] rotations of every grant, plus anything younger than [floor] whatever its rank, up to a
   * hard ceiling of [hardMax] rows per grant.
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
        AND (
          ranked.rn > :hardMax
          OR (ranked.rn > :keep AND (t.superseded_at IS NULL OR t.superseded_at < :floor))
        )
    """,
    nativeQuery = true,
  )
  fun deleteBeyondNewestPerGrant(
    @Param("keep") keep: Int,
    @Param("floor") floor: Date,
    @Param("hardMax") hardMax: Int,
  ): Int
}
