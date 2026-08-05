package io.tolgee.unit

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.model.notifications.Notification
import io.tolgee.service.notification.TaskEmailComposer
import io.tolgee.testing.assert
import io.tolgee.util.I18n
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class TaskEmailComposerTest {
  private val frontendUrlProvider =
    mock<FrontendUrlProvider> {
      on { getTaskUrl(any(), any()) } doReturn "https://app.tolgee.io/task"
      on { getMyTasksUrl() } doReturn "https://app.tolgee.io/my-tasks"
    }

  private val composer = TaskEmailComposer(frontendUrlProvider, I18n())

  @Test
  fun `escapes task name`() {
    val email = composer.composeEmail(notification(taskName = "<h1>pwned</h1>"))
    email.assert.doesNotContain("<h1>pwned</h1>")
    email.assert.contains("&lt;h1&gt;pwned&lt;/h1&gt;")
  }

  @Test
  fun `escapes language name`() {
    val email = composer.composeEmail(notification(languageName = """<a href="https://evil.example">English</a>"""))
    email.assert.doesNotContain("""<a href="https://evil.example">""")
    email.assert.contains("&lt;a href=&quot;https://evil.example&quot;&gt;")
  }

  private fun notification(
    taskName: String = "Translate",
    languageName: String = "English",
  ): Notification {
    val testData = TaskTestData()
    testData.addNotifications()
    testData.translateTask.self.apply {
      name = taskName
      language.name = languageName
    }
    return testData.taskNotification.self
  }
}
