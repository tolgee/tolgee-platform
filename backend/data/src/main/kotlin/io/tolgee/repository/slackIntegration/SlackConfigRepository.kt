package io.tolgee.repository.slackIntegration

import io.tolgee.model.slackIntegration.SlackConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SlackConfigRepository: JpaRepository<SlackConfig, Long> {
}
