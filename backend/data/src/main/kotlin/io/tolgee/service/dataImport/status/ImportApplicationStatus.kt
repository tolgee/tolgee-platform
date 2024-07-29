package io.tolgee.service.dataImport.status

enum class ImportApplicationStatus {
  ANALYZING_FILES,
  PREPARING_AND_VALIDATING,
  STORING_KEYS,
  STORING_TRANSLATIONS,
  FINALIZING,
  ERROR,
  DONE,
}
