package io.tolgee.ee.slack

import io.tolgee.activity.data.ActivityType
import io.tolgee.api.IModifiedEntityModel
import io.tolgee.api.IProjectActivityAuthorModel
import io.tolgee.api.IProjectActivityModel
import io.tolgee.ee.component.slackIntegration.data.SlackRequest
import io.tolgee.ee.component.slackIntegration.notification.SlackMessageContext
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.ApplicationContext

class SlackMessageContextTest {
  @Test
  fun `is a big operation when translations were changed but their details were not loaded`() {
    val context =
      SlackMessageContext(
        mock(ApplicationContext::class.java),
        mock(SlackConfig::class.java),
        SlackRequest(activityData = countOnlyActivity(translationCount = 100), isBigOperation = false),
      )

    context.isBigOperation.assert.isTrue()
  }

  private fun countOnlyActivity(translationCount: Long): IProjectActivityModel =
    object : IProjectActivityModel {
      override val revisionId = 0L
      override val timestamp = 0L
      override val type = ActivityType.SET_TRANSLATIONS
      override val author: IProjectActivityAuthorModel? = null
      override val modifiedEntities: Map<String, List<IModifiedEntityModel>>? = null
      override val meta: Map<String, Any?>? = null
      override val counts = mutableMapOf("Translation" to translationCount)
      override val params: Any? = null
    }
}
