package io.tolgee.hateoas.qa

import io.tolgee.model.enums.qa.QaCheckCategory
import io.tolgee.model.enums.qa.QaCheckType
import org.springframework.stereotype.Component

@Component
class QaCheckCategoryModelAssembler {
  fun toModel(
    category: QaCheckCategory,
    checkTypes: List<QaCheckType>,
  ): QaCheckCategoryModel {
    return QaCheckCategoryModel(category = category, checkTypes = checkTypes)
  }

  fun toModelsForAllCategories(): List<QaCheckCategoryModel> =
    QaCheckType.CATEGORIES.map { (category, checkTypes) -> toModel(category, checkTypes) }
}
