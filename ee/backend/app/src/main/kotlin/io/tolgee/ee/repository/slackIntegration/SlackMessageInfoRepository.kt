package io.tolgee.ee.repository.slackIntegration

import io.tolgee.model.slackIntegration.SlackMessageInfo
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface SlackMessageInfoRepository : JpaRepository<SlackMessageInfo, Long> {
  fun findBySlackMessageSlackConfigIdAndLanguageTagAndSlackMessageKeyId(
    configId: Long,
    langTag: String,
    keyId: Long,
  ): List<SlackMessageInfo>
}
