package io.tolgee.hateoas.autoTranslationConfig

import io.tolgee.api.v2.controllers.AutoTranslationController
import io.tolgee.model.AutoTranslationConfig
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class AutoTranslationSettingsModelAssembler :
  RepresentationModelAssemblerSupport<AutoTranslationConfig, AutoTranslationConfigModel>(
    AutoTranslationController::class.java,
    AutoTranslationConfigModel::class.java,
  ) {
  override fun toModel(entity: AutoTranslationConfig): AutoTranslationConfigModel {
    return AutoTranslationConfigModel(
      languageId = entity.targetLanguage?.id,
      usingTranslationMemory = entity.usingTm,
      usingMachineTranslation = entity.usingPrimaryMtService,
      enableForImport = entity.enableForImport,
    )
  }
}
