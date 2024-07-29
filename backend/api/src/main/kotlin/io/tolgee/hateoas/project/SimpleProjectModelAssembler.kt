package io.tolgee.hateoas.project

import io.tolgee.api.ISimpleProject
import io.tolgee.api.v2.controllers.project.ProjectsController
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.service.AvatarService
import io.tolgee.service.language.LanguageService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class SimpleProjectModelAssembler(
  private val languageModelAssembler: LanguageModelAssembler,
  private val avatarService: AvatarService,
  private val languageService: LanguageService,
) : RepresentationModelAssemblerSupport<ISimpleProject, SimpleProjectModel>(
    ProjectsController::class.java,
    SimpleProjectModel::class.java,
  ) {
  override fun toModel(project: ISimpleProject): SimpleProjectModel {
    return SimpleProjectModel(
      id = project.id,
      name = project.name,
      description = project.description,
      slug = project.slug,
      avatar = avatarService.getAvatarLinks(project.avatarHash),
      // it's cached so it's fast
      baseLanguage =
        languageService.getProjectLanguages(project.id).find { it.base }?.let {
          languageModelAssembler.toModel(
            LanguageDto.fromEntity(it, it.id),
          )
        },
      icuPlaceholders = project.icuPlaceholders,
    )
  }
}
