package io.tolgee.ee.component

import io.tolgee.model.UserAccount
import io.tolgee.model.views.TaskWithScopeView
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class TaskReportHelperTest {
  private val helper = TaskReportHelper(mock<TaskWithScopeView>(), emptyList())

  @Test
  fun `formatUserName carries the name and id (to disambiguate same-named users), never the e-mail`() {
    val user = UserAccount(id = 7L, name = "Franta Dobrota", username = "franta@test.com")
    val result = helper.formatUserName(user)
    assertThat(result).isEqualTo("Franta Dobrota #7")
    assertThat(result).doesNotContain("franta@test.com")
  }

  @Test
  fun `formatUserName falls back to the id when the name is blank, never the e-mail`() {
    val user = UserAccount(id = 42L, name = "", username = "franta@test.com")
    val result = helper.formatUserName(user)
    assertThat(result).isEqualTo("#42")
    assertThat(result).doesNotContain("franta@test.com")
  }
}
