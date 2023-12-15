package io.tolgee.hateoas.project

import io.tolgee.api.v2.controllers.V2ProjectsController
import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.model.Project
import io.tolgee.model.views.LanguageViewImpl
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class SimpleProjectModelAssembler(
  private val languageModelAssembler: LanguageModelAssembler,
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<Project, SimpleProjectModel>(
  V2ProjectsController::class.java, SimpleProjectModel::class.java
) {
  override fun toModel(entity: Project): SimpleProjectModel {
    return SimpleProjectModel(
      id = entity.id,
      name = entity.name,
      description = entity.description,
      slug = entity.slug,
      avatar = avatarService.getAvatarLinks(entity.avatarHash),
      baseLanguage = entity.baseLanguage?.let {
        languageModelAssembler.toModel(
          LanguageViewImpl(it, true)
        )
      },
    )
  }
}
