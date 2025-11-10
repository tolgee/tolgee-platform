package io.tolgee.repository

import io.tolgee.model.Invitation
import io.tolgee.model.Organization
import io.tolgee.model.Project
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.Optional

@Repository
@Lazy
interface InvitationRepository : JpaRepository<Invitation?, Long?> {
  fun deleteAllByCreatedAtLessThan(date: Date)

  fun findOneByCode(code: String?): Optional<Invitation>

  fun findAllByPermissionProjectOrderByCreatedAt(project: Project): LinkedHashSet<Invitation>

  @Query(
    """
    from Invitation i
    left join fetch i.organizationRole orl
    left join fetch i.permission p
    left join fetch i.createdBy
    where i.organizationRole.organization = :organization
    order by i.createdAt
  """,
  )
  fun getAllForOrganization(organization: Organization): List<Invitation>

  @Query(
    """
    select count(p) from Permission p
    left join p.invitation i
    left join p.user u
    where 
        (i.email = :email or u.username = :email) and
        p.project = :project
  """,
  )
  fun countByUserOrInvitationWithEmailAndProject(
    email: String,
    project: Project,
  ): Int

  @Query(
    """
    select count(orl.id) from OrganizationRole orl
    left join orl.invitation i
    left join orl.user u
    where 
        (i.email = :email or u.username = :email) and
        orl.organization = :organization
  """,
  )
  fun countByUserOrInvitationWithEmailAndOrganization(
    email: String,
    organization: Organization,
  ): Int
}
