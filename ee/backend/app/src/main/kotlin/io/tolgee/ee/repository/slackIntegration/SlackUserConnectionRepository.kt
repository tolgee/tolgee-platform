package io.tolgee.ee.repository.slackIntegration

import io.tolgee.model.slackIntegration.SlackUserConnection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SlackUserConnectionRepository : JpaRepository<SlackUserConnection, Long> {
  fun findBySlackUserId(slackUserId: String): SlackUserConnection?

  fun findBySlackUserIdAndSlackTeamId(
    slackUserId: String,
    slackTeamdId: String,
  ): SlackUserConnection?

  fun findByUserAccountId(userAccountId: Long): SlackUserConnection?
}
