package io.tolgee.ee.component

import io.tolgee.model.UserAccount
import io.tolgee.model.views.TaskWithScopeView
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class TaskReportHelperTest {
  private val helper = TaskReportHelper(mock<TaskWithScopeView>(), emptyList())

  @Test
  fun `formatUserName returns the name and never the e-mail`() {
    val user = UserAccount(name = "Franta Dobrota", username = "franta@test.com")
    assertThat(helper.formatUserName(user)).isEqualTo("Franta Dobrota")
  }

  @Test
  fun `formatUserName falls back to the id when the name is blank, never the e-mail`() {
    val user = UserAccount(id = 42L, name = "", username = "franta@test.com")
    val result = helper.formatUserName(user)
    assertThat(result).isEqualTo("#42")
    assertThat(result).doesNotContain("franta@test.com")
  }
}
