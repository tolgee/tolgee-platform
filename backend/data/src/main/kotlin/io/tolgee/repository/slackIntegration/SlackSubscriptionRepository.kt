package io.tolgee.repository.slackIntegration

import io.tolgee.model.slackIntegration.SlackSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SlackSubscriptionRepository : JpaRepository<SlackSubscription, Long> {
  fun findBySlackUserId(slackUserId: String): SlackSubscription?
}
