package io.tolgee.repository.apps

import io.tolgee.model.apps.App
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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
   * Apps a server admin has offered to every organization that [organizationId] neither owns nor has
   * already installed — the ones it can still add from the "Available on this server" list.
   */
  @Query(
    """
    select a from App a
    where a.availableToAllOrganizations = true
      and a.organization.id <> :organizationId
      and not exists (
        select 1 from AppInstall i where i.app = a and i.organization.id = :organizationId
      )
    order by a.name asc
    """,
  )
  fun findAvailableToInstall(
    @Param("organizationId") organizationId: Long,
  ): List<App>

  /**
   * Ids only, ascending, so the manifest reaper can walk every app in bounded batches without
   * holding a transaction open across the HTTP fetches it does between them.
   */
  @Query("select a.id from App a where a.id > :afterId order by a.id")
  fun findIdsAfter(
    @Param("afterId") afterId: Long,
    limit: Limit,
  ): List<Long>
}
