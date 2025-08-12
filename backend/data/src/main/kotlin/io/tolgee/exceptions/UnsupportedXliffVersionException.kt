package io.tolgee.exceptions

class UnsupportedXliffVersionException(version: String) : Exception() {
  override val message: String = "XLIFF version $version not supported."
}
