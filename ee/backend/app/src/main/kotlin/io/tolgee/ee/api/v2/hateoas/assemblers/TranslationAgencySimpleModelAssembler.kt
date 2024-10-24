package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.ee.api.v2.controllers.TaskController
import io.tolgee.hateoas.TranslationAgencySimpleModel
import io.tolgee.model.translationAgency.TranslationAgency
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationAgencySimpleModelAssembler(
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<TranslationAgency, TranslationAgencySimpleModel>(
  TaskController::class.java,
  TranslationAgencySimpleModel::class.java,
) {
  override fun toModel(entity: TranslationAgency): TranslationAgencySimpleModel {
    return TranslationAgencySimpleModel(
      id = entity.id,
      name = entity.name,
      url = entity.url,
      avatar = avatarService.getAvatarLinks(entity.avatarHash),
    )
  }
}

