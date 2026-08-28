package io.tolgee.repository

import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.UserAccount
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface OrganizationRoleRepository : JpaRepository<OrganizationRole, Long> {
  fun findOneByUserIdAndOrganizationId(
    userId: Long,
    organizationId: Long,
  ): OrganizationRole?

  fun findOneByUserIdAndManagedIsTrue(userId: Long): OrganizationRole?

  @Query(
    """
    select count(orr) from OrganizationRole orr
    join orr.user ua
    where orr.organization.id = :organizationId and orr.type = io.tolgee.model.enums.OrganizationRoleType.OWNER
      and ua.id <> :excludedUserId
      and ua.disabledAt is null and ua.deletedAt is null
  """,
  )
  fun countEnabledOwnersExcludingUser(
    organizationId: Long,
    excludedUserId: Long,
  ): Long

  fun deleteByOrganization(organization: Organization)

  @Query(
    """
    select or.user from OrganizationRole or
    where or.organization = :organization
        and or.type = io.tolgee.model.enums.OrganizationRoleType.OWNER
    """,
  )
  fun getOwners(organization: Organization): List<UserAccount>
}
