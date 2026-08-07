package io.tolgee.repository.apps

import io.tolgee.model.apps.App
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppRepository : JpaRepository<App, Long> {
  fun findByAppId(appId: String): App?

  fun findByClientId(clientId: String): App?

  fun findAllByOrganizationIdOrderByNameAsc(organizationId: Long): List<App>

  fun findByIdAndOrganizationId(
    id: Long,
    organizationId: Long,
  ): App?

  /**
   * Ids only, ascending, so the manifest reaper can walk every app in bounded batches without
   * holding a transaction open across the HTTP fetches it does between them.
   */
  @Query("select a.id from App a where a.id > :afterId order by a.id")
  fun findIdsAfter(
    afterId: Long,
    pageable: Pageable,
  ): List<Long>
}
