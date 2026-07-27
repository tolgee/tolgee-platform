package io.tolgee.hateoas.contributor

import io.tolgee.api.v2.controllers.project.ProjectContributorsController
import io.tolgee.model.views.ProjectContributorView
import io.tolgee.service.AvatarService
import io.tolgee.service.security.SecurityService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ContributorModelAssembler(
  private val avatarService: AvatarService,
  private val securityService: SecurityService,
) : RepresentationModelAssemblerSupport<ProjectContributorView, ContributorModel>(
    ProjectContributorsController::class.java,
    ContributorModel::class.java,
  ) {
  override fun toModel(view: ProjectContributorView): ContributorModel {
    return ContributorModel(
      id = view.id,
      username = securityService.maskedMemberField(view.username),
      name = view.name,
      avatar = avatarService.getAvatarLinks(view.avatarHash),
      firstContributionAt = view.firstContributionAt,
      lastContributionAt = view.lastContributionAt,
      invitationPending = view.invitationPending,
    )
  }
}
