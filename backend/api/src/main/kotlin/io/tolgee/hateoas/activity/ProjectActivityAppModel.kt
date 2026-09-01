package io.tolgee.hateoas.activity

import io.tolgee.api.IProjectActivityAppModel
import org.springframework.hateoas.RepresentationModel

data class ProjectActivityAppModel(
  override val installId: Long,
  override val appId: String?,
  override val name: String?,
) : RepresentationModel<ProjectActivityAppModel>(),
  IProjectActivityAppModel
