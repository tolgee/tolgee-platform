package io.tolgee.exceptions

class UnsupportedXliffVersionException(
  version: String,
) : Exception("XLIFF version $version not supported.")
