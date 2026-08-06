package io.tolgee.repository.apps

import io.tolgee.model.apps.AppSecret
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppSecretRepository : JpaRepository<AppSecret, Long> {
  fun findAllByAppIdOrderByCreatedAtDesc(appId: Long): List<AppSecret>
}
