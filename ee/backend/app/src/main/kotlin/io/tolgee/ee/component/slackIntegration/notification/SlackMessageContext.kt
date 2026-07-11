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
    val count = modifiedTranslationsCount

    if (count > SlackAutomationMessageSender.MAX_NEW_MESSAGES_TO_SEND) {
      return@lazy true
    }

    // This happens in case that the data are considered big in the view provider and so it is not loaded
    // In that case we just also consider it big
    // However, we still need to check whether there are any translations changed
    return@lazy activityData?.let { translationChangeSizeFromModifiedEntities } == null && count > 0
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
