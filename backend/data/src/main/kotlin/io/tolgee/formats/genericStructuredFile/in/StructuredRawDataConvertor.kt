package io.tolgee.formats.genericStructuredFile.`in`

interface StructuredRawDataConvertor {
  fun convert(
    rawData: Any?,
    projectIcuPlaceholdersEnabled: Boolean,
    convertPlaceholdersToIcu: Boolean,
  ): List<StructuredRawDataConversionResult>?
}
