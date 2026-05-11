package io.tolgee.ee.api.v2.hateoas.assemblers.qa

import io.tolgee.ee.api.v2.hateoas.model.qa.QaCheckCategoryModel
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
}
