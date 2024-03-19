package io.tolgee.hateoas.dataImport

import io.tolgee.api.IImportSettings
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "importSettings", itemRelation = "importSettings")
open class ImportSettingsModel(
  settings: IImportSettings,
) : RepresentationModel<ImportSettingsModel>(), IImportSettings by settings
