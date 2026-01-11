package io.tolgee.dtos.request.dataImport

import io.tolgee.api.IImportSettings
import jakarta.validation.constraints.NotNull

class ImportSettingsRequest(
  @NotNull
  override var overrideKeyDescriptions: Boolean,
  @NotNull
  override var convertPlaceholdersToIcu: Boolean,
  @NotNull
  override var createNewKeys: Boolean,
) : IImportSettings
