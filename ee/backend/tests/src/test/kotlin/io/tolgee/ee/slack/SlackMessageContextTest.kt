package io.tolgee.ee.slack

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.ExistenceEntityDescription
import io.tolgee.api.IModifiedEntityModel
import io.tolgee.api.IProjectActivityAuthorModel
import io.tolgee.api.IProjectActivityModel
import io.tolgee.ee.component.slackIntegration.data.SlackRequest
import io.tolgee.ee.component.slackIntegration.notification.SlackMessageContext
import io.tolgee.hateoas.activity.ModifiedEntityModel
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.ApplicationContext

/**
 * Unit coverage for the "big operation" decision that gates the generic "too many translations"
 * Slack summary. The guard counts distinct keys (one message is posted per key), not raw
 * translations, so a single key changed across many languages must not collapse into the summary.
 */
class SlackMessageContextTest {
  @Test
  fun `single key changed across many languages is not a big operation`() {
    val context = contextFor(activityWithKeys(keyIds = List(8) { 1L }))
    context.isBigOperation.assert.isFalse()
  }

  @Test
  fun `exactly five distinct keys is not a big operation`() {
    val context = contextFor(activityWithKeys(keyIds = (1L..5L).toList()))
    context.isBigOperation.assert.isFalse()
  }

  @Test
  fun `more than five distinct keys is a big operation`() {
    val context = contextFor(activityWithKeys(keyIds = (1L..6L).toList()))
    context.isBigOperation.assert.isTrue()
  }

  @Test
  fun `count-only activity without loaded entities is a big operation`() {
    val context = contextFor(activityModel(modifiedEntities = null, translationCount = 16))
    context.isBigOperation.assert.isTrue()
  }

  private fun contextFor(activityModel: IProjectActivityModel): SlackMessageContext =
    SlackMessageContext(
      mock(ApplicationContext::class.java),
      mock(SlackConfig::class.java),
      SlackRequest(activityData = activityModel),
    )

  private fun activityWithKeys(keyIds: List<Long>): IProjectActivityModel {
    val translations =
      keyIds.mapIndexed { index, keyId ->
        modifiedTranslation(translationId = index.toLong(), keyId = keyId)
      }
    return activityModel(modifiedEntities = mapOf("Translation" to translations), translationCount = null)
  }

  private fun modifiedTranslation(
    translationId: Long,
    keyId: Long,
  ): IModifiedEntityModel =
    ModifiedEntityModel(
      entityClass = "Translation",
      entityId = translationId,
      relations = mapOf("key" to ExistenceEntityDescription("Key", keyId, emptyMap(), emptyMap())),
    )

  private fun activityModel(
    modifiedEntities: Map<String, List<IModifiedEntityModel>>?,
    translationCount: Long?,
  ): IProjectActivityModel =
    object : IProjectActivityModel {
      override val revisionId = 0L
      override val timestamp = 0L
      override val type = ActivityType.SET_TRANSLATIONS
      override val author: IProjectActivityAuthorModel? = null
      override val modifiedEntities = modifiedEntities
      override val meta: Map<String, Any?>? = null
      override val counts = translationCount?.let { mutableMapOf("Translation" to it) }
      override val params: Any? = null
    }
}
