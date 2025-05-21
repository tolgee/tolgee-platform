package io.tolgee.hateoas

import org.springframework.hateoas.RepresentationModel

class AiPlaygroundResultModel(
  val keyId: Long,
  val languageId: Long,
  val translation: String?,
  val contextDescription: String?,
) : RepresentationModel<AiPlaygroundResultModel>()
