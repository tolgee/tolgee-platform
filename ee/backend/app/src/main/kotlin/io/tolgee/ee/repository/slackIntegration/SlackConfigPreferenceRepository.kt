package io.tolgee.ee.repository.slackIntegration

import io.tolgee.model.slackIntegration.SlackConfigPreference
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface SlackConfigPreferenceRepository : JpaRepository<SlackConfigPreference, Long>
