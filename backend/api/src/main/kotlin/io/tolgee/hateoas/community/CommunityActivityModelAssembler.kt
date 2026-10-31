package io.tolgee.hateoas.community

import io.tolgee.api.v2.controllers.CommunityProfileController
import io.tolgee.dtos.community.CommunityActivityItem
import io.tolgee.hateoas.activity.ProjectActivityModelAssembler
import io.tolgee.hateoas.project.SimpleProjectModelAssembler
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class CommunityActivityModelAssembler(
  private val projectActivityModelAssembler: ProjectActivityModelAssembler,
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler,
) : RepresentationModelAssemblerSupport<CommunityActivityItem, CommunityActivityModel>(
    CommunityProfileController::class.java,
    CommunityActivityModel::class.java,
  ) {
  override fun toModel(item: CommunityActivityItem): CommunityActivityModel {
    val activity = projectActivityModelAssembler.toModel(item.activity)
    return CommunityActivityModel(
      revisionId = activity.revisionId,
      timestamp = activity.timestamp,
      type = activity.type,
      project = simpleProjectModelAssembler.toModel(item.project),
      modifiedEntities = activity.modifiedEntities,
      meta = activity.meta,
      counts = activity.counts,
      params = activity.params,
    )
  }
}
