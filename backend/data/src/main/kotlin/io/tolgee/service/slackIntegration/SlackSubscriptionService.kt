package io.tolgee.service.slackIntegration

import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.SlackSubscription
import io.tolgee.repository.slackIntegration.SlackSubscriptionRepository
import org.springframework.stereotype.Service

@Service
class SlackSubscriptionService(
  private val slackSubscriptionRepository: SlackSubscriptionRepository
) {
  fun get(
    id: Long,
    channelId: String
  ): SlackSubscription {
    return slackSubscriptionRepository.findById(id).get()
  }

  fun getBySlackId(
    slackId: String,
    channelId: String
  ): SlackSubscription? {
    return slackSubscriptionRepository.findBySlackUserId(slackId)
  }

  fun checkIfSlackConnected(slackId: String, channelId: String) = getBySlackId(slackId, channelId) == null

  fun create(userAccount: UserAccount, slackId: String): SlackSubscription {
    val slackSubscription = SlackSubscription()
    slackSubscription.slackUserId = slackId
    slackSubscription.userAccount = userAccount
    return slackSubscriptionRepository.save(slackSubscription)
  }

}
