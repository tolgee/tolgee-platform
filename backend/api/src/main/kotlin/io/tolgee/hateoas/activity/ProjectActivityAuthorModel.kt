package io.tolgee.hateoas.activity

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.IProjectActivityAuthorModel
import io.tolgee.dtos.Avatar
import org.springframework.hateoas.RepresentationModel

data class ProjectActivityAuthorModel(
  override val id: Long,
  override var name: String?,
  override var avatar: Avatar?,
  override var deleted: Boolean,
) : RepresentationModel<ProjectActivityAuthorModel>(),
  IProjectActivityAuthorModel {
  @Deprecated("A user's username (their e-mail) is only disclosed on the project members list.")
  @get:Schema(
    deprecated = true,
    description =
      "Deprecated: always empty. A user's username (their e-mail) is disclosed only " +
        "on the project members list, never on activity author references.",
  )
  override val username: String? = ""
}
