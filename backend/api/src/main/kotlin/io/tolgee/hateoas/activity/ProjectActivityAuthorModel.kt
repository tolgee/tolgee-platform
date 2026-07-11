package io.tolgee.hateoas.activity

import io.tolgee.api.IProjectActivityAuthorModel
import io.tolgee.dtos.Avatar
import org.springframework.hateoas.RepresentationModel

data class ProjectActivityAuthorModel(
  override val id: Long,
  override val username: String?,
  override var name: String?,
  override var avatar: Avatar?,
  override var deleted: Boolean,
) : RepresentationModel<ProjectActivityAuthorModel>(),
  IProjectActivityAuthorModel
