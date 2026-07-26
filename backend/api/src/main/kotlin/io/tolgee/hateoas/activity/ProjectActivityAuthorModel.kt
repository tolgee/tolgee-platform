package io.tolgee.hateoas.activity

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.IProjectActivityAuthorModel
import io.tolgee.api.USERNAME_FIELD_DEPRECATION
import io.tolgee.dtos.Avatar
import org.springframework.hateoas.RepresentationModel

data class ProjectActivityAuthorModel(
  override val id: Long,
  override var name: String?,
  override var avatar: Avatar?,
  override var deleted: Boolean,
) : RepresentationModel<ProjectActivityAuthorModel>(),
  IProjectActivityAuthorModel {
  @Deprecated(USERNAME_FIELD_DEPRECATION)
  @get:Schema(deprecated = true, description = USERNAME_FIELD_DEPRECATION)
  override val username: String? = ""
}
