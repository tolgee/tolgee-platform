package io.tolgee.service.dataImport

enum class OverrideMode {
  /**
   * Will not override non-recommended cases (like overriding reviewed translations when they are protected)
   * and report them as unresolved conflicts with "isOverridable: true"
   */
  RECOMMENDED,

  /**
   * Will override everything that user has permissions to override even non-recommended cases
   */
  ALL,
}
