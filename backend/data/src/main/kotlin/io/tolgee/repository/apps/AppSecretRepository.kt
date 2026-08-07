package io.tolgee.repository.apps

import io.tolgee.model.apps.AppSecret
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface AppSecretRepository : JpaRepository<AppSecret, Long> {
  fun findAllByAppIdOrderByCreatedAtDesc(appId: Long): List<AppSecret>

  fun findAllByAppIdAndRevokedAtIsNull(appId: Long): List<AppSecret>

  fun findByIdAndAppId(
    id: Long,
    appId: Long,
  ): AppSecret?

  fun countByAppIdAndRevokedAtIsNull(appId: Long): Long

  @Modifying
  @Query("update AppSecret s set s.lastUsedAt = :lastUsedAt where s.id = :id")
  fun updateLastUsedById(
    id: Long,
    lastUsedAt: Date,
  )
}
