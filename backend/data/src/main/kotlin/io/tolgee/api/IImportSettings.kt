package io.tolgee.api

import io.swagger.v3.oas.annotations.media.Schema

interface IImportSettings : IStoredImportSettings {
  @get:Schema(
    description = "If true, placeholders from other formats will be converted to ICU when possible",
  )
  var convertPlaceholdersToIcu: Boolean
}
