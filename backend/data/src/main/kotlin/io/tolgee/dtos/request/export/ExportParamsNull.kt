package io.tolgee.dtos.request.export

/**
 * This class is just a helper for creating Export query,
 * since it doesn't work properly if we are checking arrays for
 * nullability
 */
class ExportParamsNull(exportParams: ExportParams) {
  val languages: Boolean
  val filterKeyId: Boolean
  val filterKeyIdNot: Boolean
  val filterTag: Boolean
  val filterKeyPrefix: Boolean
  val filterState: Boolean
  val filterStateNot: Boolean

  init {
    languages = exportParams.languages == null
    filterKeyId = exportParams.filterKeyId == null
    filterKeyIdNot = exportParams.filterKeyIdNot == null
    filterTag = exportParams.filterTag == null
    filterKeyPrefix = exportParams.filterKeyPrefix == null
    filterState = exportParams.filterState == null
    filterStateNot = exportParams.filterStateNot == null
  }
}
