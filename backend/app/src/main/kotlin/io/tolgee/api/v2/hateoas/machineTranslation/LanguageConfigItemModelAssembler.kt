package io.tolgee.api.v2.hateoas.key

import io.tolgee.api.v2.controllers.V2ProjectsController
import io.tolgee.api.v2.hateoas.machineTranslation.LanguageConfigItemModel
import io.tolgee.model.MtServiceConfig
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LanguageConfigItemModelAssembler : RepresentationModelAssemblerSupport<MtServiceConfig, LanguageConfigItemModel>(
  V2ProjectsController::class.java,
  LanguageConfigItemModel::class.java
) {
  override fun toModel(entity: MtServiceConfig) = LanguageConfigItemModel(
    targetLanguageId = entity.targetLanguage?.id,
    targetLanguageTag = entity.targetLanguage?.tag,
    targetLanguageName = entity.targetLanguage?.name,
    primaryService = entity.primaryService,
    enabledServices = entity.enabledServices
  )
}
