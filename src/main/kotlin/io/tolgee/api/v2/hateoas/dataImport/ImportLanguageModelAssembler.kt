package io.tolgee.api.v2.hateoas.dataImport

import io.tolgee.api.v2.controllers.V2ImportController
import io.tolgee.model.views.ImportLanguageView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ImportLanguageModelAssembler : RepresentationModelAssemblerSupport<ImportLanguageView, ImportLanguageModel>(
        V2ImportController::class.java, ImportLanguageModel::class.java) {
    override fun toModel(view: ImportLanguageView): ImportLanguageModel {
        return ImportLanguageModel(
                id = view.id,
                name = view.name,
                existingLanguageId = view.existingLanguageId,
                existingLanguageAbbreviation = view.existingLanguageAbbreviation,
                existingLanguageName = view.existingLanguageName,
                importFileName = view.importFileName,
                importFileId = view.importFileId,
                totalCount = view.totalCount,
                conflictCount = view.conflictCount
        )
    }
}
