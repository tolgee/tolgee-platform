package io.tolgee.api

import io.swagger.v3.oas.annotations.media.Schema

interface IImportSettings {
  @get:Schema(
    description = "If true, key descriptions will be overridden by the import",
  )
  var overrideKeyDescriptions: Boolean

  @get:Schema(
    description = "If true, only updates keys, skipping the creation of new keys",
  )
  var onlyUpdateWithoutAdd: Boolean

  @get:Schema(
    description = "If true, placeholders from other formats will be converted to ICU when possible",
  )
  var convertPlaceholdersToIcu: Boolean

  fun assignFrom(other: IImportSettings) {
    this.overrideKeyDescriptions = other.overrideKeyDescriptions
    this.convertPlaceholdersToIcu = other.convertPlaceholdersToIcu
    this.onlyUpdateWithoutAdd = other.onlyUpdateWithoutAdd
  }

  fun clone(): IImportSettings {
    return object : IImportSettings {
      override var overrideKeyDescriptions: Boolean = this@IImportSettings.overrideKeyDescriptions
      override var convertPlaceholdersToIcu: Boolean = this@IImportSettings.convertPlaceholdersToIcu
      override var onlyUpdateWithoutAdd: Boolean = this@IImportSettings.onlyUpdateWithoutAdd
    }
  }
}
