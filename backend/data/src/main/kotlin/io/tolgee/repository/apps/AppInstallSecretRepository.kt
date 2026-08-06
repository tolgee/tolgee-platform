package io.tolgee.repository.apps

import io.tolgee.model.apps.AppInstallSecret
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface AppInstallSecretRepository : JpaRepository<AppInstallSecret, Long> {
  fun findAllByAppInstallIdAndRevokedAtIsNull(appInstallId: Long): List<AppInstallSecret>

  fun findAllByAppInstallIdOrderByCreatedAtDesc(appInstallId: Long): List<AppInstallSecret>

  fun findByIdAndAppInstallId(
    id: Long,
    appInstallId: Long,
  ): AppInstallSecret?

  fun countByAppInstallIdAndRevokedAtIsNull(appInstallId: Long): Long

  @Modifying
  @Query("update AppInstallSecret s set s.lastUsedAt = :lastUsedAt where s.id = :id")
  fun updateLastUsedById(
    id: Long,
    lastUsedAt: Date,
  )
}
