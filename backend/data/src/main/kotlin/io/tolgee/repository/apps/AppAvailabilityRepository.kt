package io.tolgee.repository.apps

import io.tolgee.model.apps.AppAvailability
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppAvailabilityRepository : JpaRepository<AppAvailability, Long> {
  /** The whole availability set of an app: the sentinel row (if any) and every specific-org row. */
  fun findByAppId(appId: Long): List<AppAvailability>

  /** Every specific-org row, ordered by organization name for the management view. */
  fun findByAppIdAndOrganizationIsNotNullOrderByOrganizationNameAsc(appId: Long): List<AppAvailability>

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

  @Modifying(clearAutomatically = true)
  fun deleteByAppId(appId: Long)
}
