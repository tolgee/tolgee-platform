package io.tolgee.ee.unit

import io.tolgee.ee.component.TaskReportHelper
import io.tolgee.model.UserAccount
import io.tolgee.model.views.TaskWithScopeView
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class TaskReportHelperTest {
  private fun helper(maskEmail: (String?) -> String) =
    TaskReportHelper(
      task = mock<TaskWithScopeView>(),
      report = emptyList(),
      maskEmail = maskEmail,
    )

  private fun user(
    name: String,
    username: String,
  ) = UserAccount(username = username, name = name)

  @Test
  fun `omits the address when it is masked away`() {
    val result = helper { "" }.formatUserName(user(name = "Jane", username = "jane@tolgee.io"))

    assertThat(result).isEqualTo("Jane")
  }

  @Test
  fun `appends the address when it is exposed`() {
    val result =
      helper { it ?: "" }.formatUserName(user(name = "Jane", username = "jane@tolgee.io"))

    assertThat(result).isEqualTo("Jane (jane@tolgee.io)")
  }

  @Test
  fun `falls back to the id when the name is blank and the address is masked away`() {
    val user = user(name = "", username = "jane@tolgee.io").apply { id = 42 }

    assertThat(helper { "" }.formatUserName(user)).isEqualTo("#42")
  }

  @Test
  fun `omits the address when it equals the name`() {
    val result = helper { it ?: "" }.formatUserName(user(name = "Jane", username = "Jane"))

    assertThat(result).isEqualTo("Jane")
  }
}
