package io.tolgee.hateoas.dataImport

import io.tolgee.api.v2.controllers.dataImport.V2ImportController
import io.tolgee.model.views.ImportFileIssueParamView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ImportFileIssueParamModelAssembler :
  RepresentationModelAssemblerSupport<ImportFileIssueParamView, ImportFileIssueParamModel>(
    V2ImportController::class.java,
    ImportFileIssueParamModel::class.java,
  ) {
  override fun toModel(view: ImportFileIssueParamView): ImportFileIssueParamModel {
    return ImportFileIssueParamModel(
      type = view.type,
      value = view.value,
    )
  }
}
