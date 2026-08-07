package io.tolgee.model.apps

/** Why a periodic manifest check of an [App] failed. */
enum class AppManifestFailureKind {
  /** Nothing answered at the manifest URL, or what answered was not a successful HTTP response. */
  UNREACHABLE,

  /** The URL answered, but the document it served is not a manifest Tolgee accepts any more. */
  INVALID,
}
