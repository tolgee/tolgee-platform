package io.tolgee.ee.component.slackIntegration.notification

import io.tolgee.api.IProjectActivityModel
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.ee.component.slackIntegration.SlackChannelMessagesOperations
import io.tolgee.ee.component.slackIntegration.SlackNotConfiguredException
import io.tolgee.ee.component.slackIntegration.data.SlackRequest
import io.tolgee.ee.service.slackIntegration.SlackUserConnectionService
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.service.language.LanguageService
import org.springframework.context.ApplicationContext

class SlackMessageContext(
  private val applicationContext: ApplicationContext,
  val slackConfig: SlackConfig,
  val data: SlackRequest,
) {
  val slackToken: SlackChannelMessagesOperations.SlackToken
    get() = SlackChannelMessagesOperations.SlackWorkspaceToken(slackConfig.organizationSlackWorkspace.getSlackToken())

  val baseLanguage by lazy {
    languageService.getProjectBaseLanguage(slackConfig.project.id)
  }

  val activityData: IProjectActivityModel?
    get() = data.activityData

  val author: String? by lazy {
    getSlackNickName(activityData?.author?.id ?: 0L)
  }

  val authorMention: String? by lazy {
    author ?: activityData?.author?.name
  }

  val isBigOperation: Boolean by lazy {
    // A Slack message is posted per key, so the flood guard counts distinct keys, not raw
    // translations. One key changed across many languages (e.g. a base edit that auto-translates
    // into every target language) is a single message, not a flood, and must not collapse into the
    // generic "too many translations" summary.
    val keyCount = modifiedKeyCount
    if (keyCount != null) {
      return@lazy keyCount > SlackAutomationMessageSender.MAX_NEW_MESSAGES_TO_SEND
    }

    // Bulk activity types (AUTO_TRANSLATE, batch operations) expose only aggregate counts in the
    // activity view provider — the per-entity detail is not loaded, so distinct keys cannot be
    // counted. Treat such an activity as big whenever anything was changed.
    return@lazy modifiedTranslationsCount > 0
  }

  /**
   * Number of distinct keys touched by this activity, or null when per-entity detail is not loaded
   * (bulk activity types), in which case only aggregate counts are available.
   */
  private val modifiedKeyCount: Int? by lazy {
    val translations = activityData?.modifiedEntities?.get("Translation")
    if (translations.isNullOrEmpty()) {
      return@lazy null
    }
    translations.mapNotNull { it.relations?.get("key")?.entityId }.distinct().size
  }

  val modifiedTranslationsCount: Long by lazy {
    val activityData = activityData ?: return@lazy 0

    val countFromCounts = activityData.counts?.get("Translation")

    // for activities with a lot of data, we get count only in the counts map
    if (countFromCounts != null) {
      return@lazy countFromCounts
    }

    // for small activities, we get count from modifiedEntities
    return@lazy translationChangeSizeFromModifiedEntities
  }

  /**
   * If this is empty, it means that the operation is probably big
   */
  private val translationChangeSizeFromModifiedEntities: Long by lazy {
    activityData
      ?.modifiedEntities
      ?.get("Translation")
      ?.size
      ?.toLong() ?: 0L
  }

  private fun getSlackNickName(authorId: Long): String? {
    val slackId = slackUserConnectionService.findByUserAccountId(authorId)?.slackUserId ?: return null
    return "<@$slackId>"
  }

  fun shouldSkipModification(languageTag: String): Boolean {
    val preferences = slackConfig.preferences
    val globalSubscription = slackConfig.isGlobalSubscription

    val languageTagsSet = preferences.map { it.languageTag }.toSet()
    return !globalSubscription &&
      !languageTagsSet.contains(languageTag) &&
      baseLanguage.tag != languageTag
  }

  val dataProvider by lazy {
    SlackIntegrationDataProvider(applicationContext)
  }

  private val tolgeeProperties by lazy {
    applicationContext.getBean(TolgeeProperties::class.java)
  }

  private val languageService by lazy {
    applicationContext.getBean(LanguageService::class.java)
  }

  private val slackUserConnectionService by lazy {
    applicationContext.getBean(SlackUserConnectionService::class.java)
  }

  private fun OrganizationSlackWorkspace?.getSlackToken(): String {
    return this?.accessToken ?: tolgeeProperties.slack.token ?: throw SlackNotConfiguredException()
  }
}
