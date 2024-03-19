package io.tolgee.hateoas.dataImport

import io.tolgee.api.v2.controllers.dataImport.V2ImportController
import io.tolgee.model.views.ImportFileIssueView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ImportFileIssueModelAssembler(
  val importFileIssueParamModelAssembler: ImportFileIssueParamModelAssembler,
) : RepresentationModelAssemblerSupport<ImportFileIssueView, ImportFileIssueModel>(
    V2ImportController::class.java,
    ImportFileIssueModel::class.java,
  ) {
  override fun toModel(view: ImportFileIssueView): ImportFileIssueModel {
    return ImportFileIssueModel(
      id = view.id,
      type = view.type,
      params = view.params.map { importFileIssueParamModelAssembler.toModel(it) },
    )
  }
}
