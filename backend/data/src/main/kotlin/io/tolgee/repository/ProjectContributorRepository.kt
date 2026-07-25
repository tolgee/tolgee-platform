package io.tolgee.repository

import io.tolgee.model.contributor.ProjectContributor
import io.tolgee.model.contributor.ProjectContributorId
import io.tolgee.model.views.ProjectContributorView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository

interface ProjectContributorRepository : Repository<ProjectContributor, ProjectContributorId> {
  @Query(
    """
      select u.id as id, u.username as username, u.name as name, u.avatarHash as avatarHash,
        pc.firstContributionAt as firstContributionAt, pc.lastContributionAt as lastContributionAt,
        (exists (
          select 1 from Invitation i
          join i.permission ip
          where ip.project.id = pc.projectId and lower(i.email) = lower(u.username)
        )) as invitationPending
      $VISIBLE_CONTRIBUTOR
    """,
  )
  fun findContributors(
    projectId: Long,
    pageable: Pageable,
  ): Page<ProjectContributorView>

  companion object {
    // Inverse of UserAccountRepository.getAllInProject; pinned by ProjectContributorsControllerTest
    // "partitions every membership shape into exactly one of the members and contributors lists".
    const val VISIBLE_CONTRIBUTOR =
      """
      from ProjectContributor pc
      join UserAccount u on u.id = pc.userId
      left join Project r on r.id = pc.projectId
      left join u.permissions p on p.project.id = pc.projectId
      left join u.organizationRoles orl on orl.organization = r.organizationOwner
      where pc.projectId = :projectId
        and p is null
        and orl is null
        and u.deletedAt is null
        and u.disabledAt is null
      """
  }
}
