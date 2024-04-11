package io.tolgee.repository.slackIntegration

import io.tolgee.model.slackIntegration.SlackUserConnection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SlackUserConnectionRepository : JpaRepository<SlackUserConnection, Long> {
  fun findBySlackUserId(slackUserId: String): SlackUserConnection?
}
