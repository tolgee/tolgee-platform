package io.tolgee.service.dataImport.status

enum class ImportApplicationStatus {
  PREPARING_AND_VALIDATING,
  STORING_KEYS,
  STORING_TRANSLATIONS,
  FINALIZING,
}
