package io.tolgee.repository.slackIntegration

import io.tolgee.model.slackIntegration.SavedSlackMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Transactional
@Repository
interface SavedSlackMessageRepository : JpaRepository<SavedSlackMessage, Long> {
  @Modifying
  @Query(
    """
    delete from SavedSlackMessage sm where sm.createdAt < :cutoff
    """,
  )
  fun deleteOlderThan(cutoff: Date)

  fun findByKeyIdAndSlackConfigId(
    keyId: Long,
    configId: Long,
  ): List<SavedSlackMessage>

  @Query(
    """
    SELECT * FROM saved_slack_message m
    WHERE m.key_id = :keyId AND m.slack_config_id = :configId
    AND m.lang_tags @> CAST(:langTags AS jsonb)
""",
    nativeQuery = true,
  )
  fun findByKeyIdAndConfigIdAndLangTags(
    @Param("keyId") keyId: Long,
    @Param("configId") configId: Long,
    @Param("langTags") langTags: String,
  ): List<SavedSlackMessage>
}
