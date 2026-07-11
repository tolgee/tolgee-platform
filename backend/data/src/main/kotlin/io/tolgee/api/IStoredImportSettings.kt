package io.tolgee.api

import io.swagger.v3.oas.annotations.media.Schema

interface IStoredImportSettings {
  @get:Schema(
    description = "If true, key descriptions will be overridden by the import",
  )
  var overrideKeyDescriptions: Boolean

  @get:Schema(
    description = "If false, only updates keys, skipping the creation of new keys",
  )
  var createNewKeys: Boolean

  fun assignFrom(other: IStoredImportSettings) {
    this.overrideKeyDescriptions = other.overrideKeyDescriptions
    this.createNewKeys = other.createNewKeys
  }

  fun storedClone(): IStoredImportSettings {
    return object : IStoredImportSettings {
      override var overrideKeyDescriptions: Boolean = this@IStoredImportSettings.overrideKeyDescriptions
      override var createNewKeys: Boolean = this@IStoredImportSettings.createNewKeys
    }
  }
}
