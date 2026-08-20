package io.tolgee.repository.apps

import io.tolgee.model.apps.AppSecret
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface AppSecretRepository : JpaRepository<AppSecret, Long> {
  fun findAllByAppIdOrderByCreatedAtDesc(appId: Long): List<AppSecret>

  fun findByIdAndAppId(
    id: Long,
    appId: Long,
  ): AppSecret?

  /** Secrets that still authenticate: not revoked and not past their expiry. */
  @Query(
    """
    select s from AppSecret s
    where s.app.id = :appId and s.revokedAt is null
      and (s.expiresAt is null or s.expiresAt > :now)
    """,
  )
  fun findActiveByAppId(
    @Param("appId") appId: Long,
    @Param("now") now: Date,
  ): List<AppSecret>

  @Query(
    """
    select count(s) from AppSecret s
    where s.app.id = :appId and s.revokedAt is null
      and (s.expiresAt is null or s.expiresAt > :now)
    """,
  )
  fun countActiveByAppId(
    @Param("appId") appId: Long,
    @Param("now") now: Date,
  ): Long

  @Modifying
  @Query("update AppSecret s set s.lastUsedAt = :lastUsedAt where s.id = :id")
  fun updateLastUsedById(
    @Param("id") id: Long,
    @Param("lastUsedAt") lastUsedAt: Date,
  )
}
