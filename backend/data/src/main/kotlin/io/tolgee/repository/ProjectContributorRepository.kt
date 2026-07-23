package io.tolgee.repository

import io.tolgee.model.contributor.ProjectContributor
import io.tolgee.model.contributor.ProjectContributorId
import io.tolgee.model.views.ProjectContributorView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository

interface ProjectContributorRepository : Repository<ProjectContributor, ProjectContributorId> {
  // The `p is null and orl is null` membership exclusion must stay the exact inverse of the
  // `p is not null or orl is not null` inclusion in UserAccountRepository.getAllInProject.
  @Query(
    """
      select u.id as id, u.name as name, u.avatarHash as avatarHash,
        pc.firstContributionAt as firstContributionAt, pc.lastContributionAt as lastContributionAt
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
    """,
  )
  fun findContributors(
    projectId: Long,
    pageable: Pageable,
  ): Page<ProjectContributorView>

  // Membership exclusion (`p is null and orl is null`) must match the mine-only filter in
  // ProjectRepository.findAllPublic — the switcher entry and the community page share this predicate.
  @Query(
    """
      select count(pc) > 0
      from ProjectContributor pc
      join Project r on r.id = pc.projectId
      left join r.baseLanguage bl
      left join r.organizationOwner o
      left join Permission p on p.project = r and p.user.id = :userId
      left join OrganizationRole orl on orl.organization = o and orl.user.id = :userId
      where pc.userId = :userId
        and ${ProjectRepository.PUBLIC_PROJECT_VISIBILITY}
        and p is null
        and orl is null
    """,
  )
  fun hasNonMemberPublicContribution(userId: Long): Boolean
}
