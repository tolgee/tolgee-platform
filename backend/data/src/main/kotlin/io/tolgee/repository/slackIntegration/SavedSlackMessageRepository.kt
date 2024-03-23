package io.tolgee.repository.slackIntegration

import io.tolgee.model.slackIntegration.SavedSlackMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
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
}
