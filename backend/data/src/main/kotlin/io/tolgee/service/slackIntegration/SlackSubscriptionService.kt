package io.tolgee.service.slackIntegration

import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.SlackSubscription
import io.tolgee.repository.slackIntegration.SlackSubscriptionRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class SlackSubscriptionService(
  private val slackSubscriptionRepository: SlackSubscriptionRepository,
) {
  fun get(
    id: Long,
    channelId: String,
  ): SlackSubscription {
    return slackSubscriptionRepository.findById(id).get()
  }

  fun getBySlackId(slackId: String): SlackSubscription? {
    return slackSubscriptionRepository.findBySlackUserId(slackId)
  }

  @Transactional
  fun delete(slackId: String): Boolean {
    val subscription = getBySlackId(slackId) ?: return false
    slackSubscriptionRepository.delete(subscription)
    return true
  }

  fun ifSlackConnected(slackId: String) = getBySlackId(slackId) != null

  fun create(
    userAccount: UserAccount,
    slackId: String,
    slackNickName: String,
  ): SlackSubscription {
    val slackSubscription = SlackSubscription()
    slackSubscription.slackUserId = slackId
    slackSubscription.userAccount = userAccount
    slackSubscription.slackNickName = slackNickName
    return slackSubscriptionRepository.save(slackSubscription)
  }
}
