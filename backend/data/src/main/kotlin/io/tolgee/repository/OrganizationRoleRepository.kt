package io.tolgee.repository

import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
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

  fun countAllByOrganizationIdAndTypeAndUserIdNot(
    id: Long,
    owner: OrganizationRoleType,
    userId: Long,
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
