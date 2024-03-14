package io.tolgee.formats.genericStructuredFile.`in`

interface StructuredRawDataConvertor {
  fun convert(
    keyPrefix: String,
    rawData: Any?,
    projectIcuPlaceholdersEnabled: Boolean,
    convertPlaceholdersToIcu: Boolean,
  ): List<StructuredRawDataConversionResult>?
}
