package io.tolgee.hateoas.dataImport

import io.tolgee.api.v2.controllers.dataImport.V2ImportController
import io.tolgee.formats.convertToIcuPlural
import io.tolgee.model.enums.ConflictType
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
    val text = getText(view)

    return ImportTranslationModel(
      id = view.id,
      text = text,
      keyName = view.keyName,
      keyId = view.keyId,
      conflictId = view.conflictId,
      conflictText = view.conflictText,
      override = view.override,
      resolved = view.resolvedHash != null,
      keyDescription = view.keyDescription,
      isPlural = view.plural,
      existingKeyIsPlural = view.existingKeyPlural ?: false,
      conflictType = view.conflictType,
      isOverridable = ConflictType.isOverridable(view.conflictType),
    )
  }

  fun getText(view: ImportTranslationView): String? {
    if (view.plural) {
      return view.text
    }
    if (view.existingKeyPlural == true) {
      return view.text?.convertToIcuPlural(null)
    }
    return view.text
  }
}
