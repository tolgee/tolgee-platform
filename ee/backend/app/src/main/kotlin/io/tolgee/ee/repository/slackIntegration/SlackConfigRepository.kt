package io.tolgee.ee.repository.slackIntegration

import io.tolgee.model.slackIntegration.SlackConfig
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Lazy
interface SlackConfigRepository : JpaRepository<SlackConfig, Long> {
  fun findByProjectIdAndChannelId(
    id: Long,
    channelId: String,
  ): SlackConfig?

  @Transactional
  @Query("SELECT sc FROM SlackConfig sc WHERE sc.channelId = :channelId and sc.project.id = :projectId")
  fun find(
    @Param("projectId") projectId: Long,
    @Param("channelId") channelId: String,
  ): SlackConfig?

  fun getAllByChannelId(channelId: String): List<SlackConfig>
}
