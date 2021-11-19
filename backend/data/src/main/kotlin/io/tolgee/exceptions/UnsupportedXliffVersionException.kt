package io.tolgee.exceptions

class UnsupportedXliffVersionException(version: String) : Throwable() {
  override val message: String = "XLIFF version $version not supported."
}
