package io.tolgee.hateoas

import io.tolgee.model.translationAgency.TranslationAgency
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class TranslationAgencySimpleModelAssembler(
  private val avatarService: AvatarService,
) : RepresentationModelAssembler<TranslationAgency, TranslationAgencySimpleModel> {
  override fun toModel(entity: TranslationAgency): TranslationAgencySimpleModel {
    return TranslationAgencySimpleModel(
      id = entity.id,
      name = entity.name,
      url = entity.url,
      avatar = avatarService.getAvatarLinks(entity.avatarHash),
    )
  }
}
