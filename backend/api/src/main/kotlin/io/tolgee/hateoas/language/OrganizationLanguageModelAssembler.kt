package io.tolgee.hateoas.language

import io.tolgee.api.v2.controllers.organization.OrganizationLanguageController
import io.tolgee.dtos.cacheable.OrganizationLanguageDto
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class OrganizationLanguageModelAssembler :
  RepresentationModelAssemblerSupport<OrganizationLanguageDto, OrganizationLanguageModel>(
    OrganizationLanguageController::class.java,
    OrganizationLanguageModel::class.java,
  ) {
  override fun toModel(languageDto: OrganizationLanguageDto): OrganizationLanguageModel {
    return OrganizationLanguageModel(
      name = languageDto.name,
      originalName = languageDto.originalName,
      tag = languageDto.tag,
      flagEmoji = languageDto.flagEmoji,
      base = languageDto.base,
    )
  }
}
