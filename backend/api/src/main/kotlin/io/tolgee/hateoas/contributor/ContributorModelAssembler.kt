package io.tolgee.hateoas.contributor

import io.tolgee.api.v2.controllers.project.ProjectContributorsController
import io.tolgee.model.views.ProjectContributorView
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ContributorModelAssembler(
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<ProjectContributorView, ContributorModel>(
    ProjectContributorsController::class.java,
    ContributorModel::class.java,
  ) {
  override fun toModel(view: ProjectContributorView): ContributorModel {
    return ContributorModel(
      id = view.id,
      name = view.name,
      avatar = avatarService.getAvatarLinks(view.avatarHash),
      firstContributionAt = view.firstContributionAt,
      lastContributionAt = view.lastContributionAt,
    )
  }
}
