package io.tolgee.dtos.dataImport

import java.io.Serializable

data class SimpleImportConflictResult(
  val keyName: String,
  val keyNamespace: String? = null,
  val language: String,
  val isOverridable: Boolean,
) : Serializable
