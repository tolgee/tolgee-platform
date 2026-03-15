package io.tolgee.ee.api.v2.hateoas.model.qa

import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "qaSettings", itemRelation = "qaSettings")
class QaSettingsModel(
  val settings: Map<QaCheckType, QaCheckSeverity>,
) : RepresentationModel<QaSettingsModel>()
