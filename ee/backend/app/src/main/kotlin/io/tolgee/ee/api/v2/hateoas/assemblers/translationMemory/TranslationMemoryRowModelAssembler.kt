package io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory

import io.tolgee.ee.api.v2.controllers.translationMemory.TranslationMemoryEntryController
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryRowCellModel
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryRowModel
import io.tolgee.ee.service.translationMemory.TmRow
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationMemoryRowModelAssembler :
  RepresentationModelAssemblerSupport<TmRow, TranslationMemoryRowModel>(
    TranslationMemoryEntryController::class.java,
    TranslationMemoryRowModel::class.java,
  ) {
  override fun toModel(entity: TmRow): TranslationMemoryRowModel {
    val cells =
      when (entity.kind) {
        TmRow.Kind.STORED ->
          entity.entries.map {
            TranslationMemoryRowCellModel(
              targetText = it.targetText,
              targetLanguageTag = it.targetLanguageTag,
              entryId = it.entryId,
            )
          }
        TmRow.Kind.VIRTUAL ->
          entity.virtualEntries.map {
            TranslationMemoryRowCellModel(
              targetText = it.targetText,
              targetLanguageTag = it.targetLanguageTag,
              entryId = null,
            )
          }
      }
    return TranslationMemoryRowModel(
      sourceText = entity.sourceText,
      editable = entity.kind == TmRow.Kind.STORED,
      cells = cells,
      keyName = entity.keyName,
      projectId = entity.projectId,
      projectName = entity.projectName,
    )
  }
}
