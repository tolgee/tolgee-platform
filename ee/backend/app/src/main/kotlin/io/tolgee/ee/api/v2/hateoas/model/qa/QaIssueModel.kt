package io.tolgee.ee.api.v2.hateoas.model.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "qaIssues", itemRelation = "qaIssue")
class QaIssueModel(
  val id: Long,
  val type: QaCheckType,
  val message: QaIssueMessage,
  val replacement: String?,
  val positionStart: Int,
  val positionEnd: Int,
  val state: QaIssueState,
  val params: Map<String, String>? = null,
) : RepresentationModel<QaIssueModel>()
