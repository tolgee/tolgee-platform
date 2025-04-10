package io.tolgee.hateoas.machineTranslation

import io.tolgee.service.machineTranslation.MtSupportedService
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "languageInfos", itemRelation = "languageInfo")
class LanguageInfoModel(
  val languageId: Long?,
  val languageTag: String?,
  val supportedServices: List<MtSupportedService>,
) : RepresentationModel<LanguageInfoModel>(),
  Serializable
