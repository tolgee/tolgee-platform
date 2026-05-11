package io.tolgee.ee.api.v2.hateoas.model.qa

import io.tolgee.model.enums.qa.QaCheckCategory
import io.tolgee.model.enums.qa.QaCheckType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "qaCheckCategories", itemRelation = "qaCheckCategory")
class QaCheckCategoryModel(
  val category: QaCheckCategory,
  val checkTypes: List<QaCheckType>,
) : RepresentationModel<QaCheckCategoryModel>()
