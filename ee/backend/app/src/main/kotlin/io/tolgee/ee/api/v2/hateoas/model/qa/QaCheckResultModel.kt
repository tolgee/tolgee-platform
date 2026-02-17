package io.tolgee.ee.api.v2.hateoas.model.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "qaCheckResults", itemRelation = "qaCheckResult")
class QaCheckResultModel(
  val type: QaCheckType,
  val message: QaIssueMessage,
  val replacement: String?,
  val positionStart: Int,
  val positionEnd: Int,
) : RepresentationModel<QaCheckResultModel>()
