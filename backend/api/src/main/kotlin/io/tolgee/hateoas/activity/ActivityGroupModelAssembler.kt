package io.tolgee.hateoas.activity

import io.tolgee.api.IProjectActivityModel
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.dtos.queryResults.ActivityGroupView
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class ActivityGroupModelAssembler(
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler
) : RepresentationModelAssembler<ActivityGroupView, ActivityGroupModel> {
  override fun toModel(view: ActivityGroupView): ActivityGroupModel {
    return ActivityGroupModel(
      id = view.id,
      timestamp = view.timestamp.time,
      type = view.type,
      author = simpleUserAccountModelAssembler.toModel(
        view.author
      ),
      counts = view.counts,
      data = view.data,
    )
  }
}
