package io.tolgee.service.contributor

import io.tolgee.model.views.ProjectContributorView
import io.tolgee.repository.ProjectContributorRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class ProjectContributorService(
  private val projectContributorRepository: ProjectContributorRepository,
) {
  fun getContributors(
    projectId: Long,
    pageable: Pageable,
  ): Page<ProjectContributorView> {
    return projectContributorRepository.findContributors(projectId, withTotalOrder(pageable))
  }

  fun hasCommunityContributions(userId: Long): Boolean {
    return projectContributorRepository.hasNonMemberPublicContribution(userId)
  }

  private fun withTotalOrder(pageable: Pageable): Pageable {
    val orders = pageable.sort.filter { it.property in ALLOWED_SORT_PROPERTIES }.toMutableList()
    if (orders.isEmpty()) {
      orders.add(Sort.Order.desc("lastContributionAt"))
    }
    orders.add(Sort.Order.asc("userId"))
    return PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(orders))
  }

  companion object {
    private val ALLOWED_SORT_PROPERTIES = setOf("lastContributionAt", "firstContributionAt")
  }
}
