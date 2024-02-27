package io.tolgee.hateoas.dataImport

import io.tolgee.api.v2.controllers.dataImport.V2ImportController
import io.tolgee.model.views.ImportTranslationView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ImportTranslationModelAssembler :
  RepresentationModelAssemblerSupport<ImportTranslationView, ImportTranslationModel>(
    V2ImportController::class.java,
    ImportTranslationModel::class.java,
  ) {
  override fun toModel(view: ImportTranslationView): ImportTranslationModel {
    return ImportTranslationModel(
      id = view.id,
      text = view.text,
      keyName = view.keyName,
      keyId = view.keyId,
      conflictId = view.conflictId,
      conflictText = view.conflictText,
      override = view.override,
      resolved = view.resolvedHash != null,
    )
  }
}
