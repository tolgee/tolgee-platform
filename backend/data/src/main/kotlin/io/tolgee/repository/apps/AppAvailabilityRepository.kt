package io.tolgee.repository.apps

import io.tolgee.model.Organization
import io.tolgee.model.apps.AppAvailability
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppAvailabilityRepository : JpaRepository<AppAvailability, Long> {
  /**
   * Adds an availability row unless the same target already exists. `on conflict do nothing` makes
   * the grant idempotent at the database, so a concurrent duplicate is a no-op instead of a caught
   * exception that would poison the transaction, while a foreign-key violation still propagates.
   * `:organizationId` null is the all-organizations sentinel.
   */
  @Modifying
  @Query(
    value = """
      insert into app_availability (id, created_at, updated_at, app_id, organization_id)
      values (nextval('hibernate_sequence'), now(), now(), :appId, :organizationId)
      on conflict do nothing
    """,
    nativeQuery = true,
  )
  fun insertIfAbsent(
    @Param("appId") appId: Long,
    @Param("organizationId") organizationId: Long?,
  )

  fun findByAppIdAndOrganizationIsNull(appId: Long): AppAvailability?

  fun findByAppIdAndOrganizationId(
    appId: Long,
    organizationId: Long,
  ): AppAvailability?

  fun existsByAppIdAndOrganizationIsNull(appId: Long): Boolean

  fun existsByAppIdAndOrganizationId(
    appId: Long,
    organizationId: Long,
  ): Boolean

  /** Which of these apps carry the all-organizations sentinel, so a list of apps does not fan out. */
  @Query(
    """
    select a.app.id from AppAvailability a
    where a.app.id in :appIds and a.organization is null
    """,
  )
  fun findAppIdsAvailableToAll(
    @Param("appIds") appIds: Collection<Long>,
  ): List<Long>

  /**
   * The organizations the app is specifically offered to, ordered (name, then id for ties) because a
   * paged select without a total order can repeat or drop rows across pages.
   */
  @Query(
    """
    select a.organization from AppAvailability a
    where a.app.id = :appEntityId and a.organization is not null
      and (:search is null
        or lower(a.organization.name) like lower(concat('%', cast(:search as text), '%'))
        or lower(a.organization.slug) like lower(concat('%', cast(:search as text), '%')))
    order by a.organization.name, a.organization.id
    """,
    countQuery = """
    select count(a) from AppAvailability a
    where a.app.id = :appEntityId and a.organization is not null
      and (:search is null
        or lower(a.organization.name) like lower(concat('%', cast(:search as text), '%'))
        or lower(a.organization.slug) like lower(concat('%', cast(:search as text), '%')))
    """,
  )
  fun findAvailableOrganizations(
    @Param("appEntityId") appEntityId: Long,
    @Param("search") search: String?,
    pageable: Pageable,
  ): Page<Organization>

  @Modifying(clearAutomatically = true)
  fun deleteByAppId(appId: Long)
}
