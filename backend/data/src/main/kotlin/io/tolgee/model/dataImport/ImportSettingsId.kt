package io.tolgee.model.dataImport

import java.io.Serializable

data class ImportSettingsId(
  val userAccount: Long? = null,
  val project: Long? = null,
) : Serializable
