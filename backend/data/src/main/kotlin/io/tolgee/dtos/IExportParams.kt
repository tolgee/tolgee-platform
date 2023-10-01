package io.tolgee.dtos

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.model.enums.TranslationState

interface IExportParams {
  var languages: Set<String>?
  var format: ExportFormat
  var structureDelimiter: Char?

  var filterKeyId: List<Long>?
  var filterKeyIdNot: List<Long>?
  var filterTag: String?
  var filterKeyPrefix: String?
  var filterState: List<TranslationState>?
  var filterNamespace: List<String?>?

  @get:Hidden
  @get:JsonIgnore
  val shouldContainUntranslated: Boolean
    get() = this.filterState?.contains(TranslationState.UNTRANSLATED) != false

  fun copyPropsFrom(other: IExportParams) {
    this.languages = other.languages
    this.format = other.format
    this.structureDelimiter = other.structureDelimiter
    this.filterKeyId = other.filterKeyId
    this.filterKeyIdNot = other.filterKeyIdNot
    this.filterTag = other.filterTag
    this.filterKeyPrefix = other.filterKeyPrefix
    this.filterState = other.filterState
    this.filterNamespace = other.filterNamespace
  }
}
