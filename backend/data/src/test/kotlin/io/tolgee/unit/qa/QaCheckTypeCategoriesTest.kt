package io.tolgee.unit.qa

import io.tolgee.model.enums.qa.QaCheckType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class QaCheckTypeCategoriesTest {
  @Test
  fun `CATEGORIES contains every QaCheckType exactly once`() {
    val allTypesInCategories = QaCheckType.CATEGORIES.values.flatten()

    assertThat(allTypesInCategories)
      .`as`("CATEGORIES must contain every QaCheckType exactly once")
      .containsExactlyInAnyOrderElementsOf(QaCheckType.entries)
  }
}
