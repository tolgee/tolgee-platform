package io.tolgee.hateoas.dataImport

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.IStoredImportSettings
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "importSettings", itemRelation = "importSettings")
open class ImportSettingsModel(
  @Schema(hidden = true)
  settings: IStoredImportSettings,
) : RepresentationModel<ImportSettingsModel>(),
  IStoredImportSettings by settings
