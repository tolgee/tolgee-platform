package io.tolgee.repository.apps

import io.tolgee.model.apps.App
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppRepository : JpaRepository<App, Long> {
  fun findByAppId(appId: String): App?
}
