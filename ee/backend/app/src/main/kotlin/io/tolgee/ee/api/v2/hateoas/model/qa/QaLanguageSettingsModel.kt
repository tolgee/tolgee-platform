package io.tolgee.ee.api.v2.hateoas.model.qa

import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "qaLanguageSettings", itemRelation = "qaLanguageSettings")
class QaLanguageSettingsModel(
  val settings: Map<QaCheckType, QaCheckSeverity>?,
) : RepresentationModel<QaLanguageSettingsModel>()
