package io.tolgee.dtos.request.dataImport

import io.tolgee.api.IStoredImportSettings
import jakarta.validation.constraints.NotNull

class ImportSettingsRequest(
  @NotNull
  override var overrideKeyDescriptions: Boolean,
  @NotNull
  override var createNewKeys: Boolean,
) : IStoredImportSettings
