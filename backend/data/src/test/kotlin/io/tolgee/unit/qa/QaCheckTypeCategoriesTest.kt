package io.tolgee.unit.qa

import io.tolgee.model.enums.qa.QaCheckType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class QaCheckTypeCategoriesTest {
  @Test
  fun `CATEGORIES contains every QaCheckType exactly once`() {
    val allTypesInCategories = QaCheckType.CATEGORIES.values.flatten()

    assertThat(allTypesInCategories.toSet())
      .`as`("CATEGORIES must contain every QaCheckType value")
      .isEqualTo(QaCheckType.entries.toSet())

    assertThat(allTypesInCategories)
      .`as`("CATEGORIES must not contain duplicate QaCheckType values")
      .hasSize(allTypesInCategories.toSet().size)
  }
}
