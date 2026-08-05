package io.tolgee.util

import java.util.Locale

/** Discriminator for the `tolgee.async.streaming.duration` metric. */
enum class StreamType {
  EXPORT_SINGLE_FILE,
  EXPORT_ZIP,
  EXPORT_JSON_ZIP,
  EXPORT_PROJECT,
  EXPORT_GLOSSARY,
  IMPORT_APPLY,
  MT_SUGGEST,

  /** Only produced by the internal test-only endpoint. */
  INTERNAL_TEST,
  ;

  val tag: String = name.lowercase(Locale.ROOT)
}
