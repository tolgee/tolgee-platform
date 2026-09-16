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

  @Modifying
  @Query("DELETE FROM OAuth2SupersededRefreshToken s WHERE s.supersededAt < :cutoff")
  fun deleteSupersededBefore(cutoff: Date): Int
}
