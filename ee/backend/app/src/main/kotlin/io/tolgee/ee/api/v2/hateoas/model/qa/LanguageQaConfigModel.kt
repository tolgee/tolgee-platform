package io.tolgee.ee.api.v2.hateoas.model.qa

import io.tolgee.hateoas.language.LanguageModel
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "languageQaConfigs", itemRelation = "languageQaConfig")
class LanguageQaConfigModel(
  val language: LanguageModel,
  val customSettings: Map<QaCheckType, QaCheckSeverity>?,
) : RepresentationModel<LanguageQaConfigModel>()
