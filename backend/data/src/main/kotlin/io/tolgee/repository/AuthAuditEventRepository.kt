package io.tolgee.repository

import io.tolgee.model.AuthAuditEvent
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface AuthAuditEventRepository : JpaRepository<AuthAuditEvent, Long> {
  @Query("select ae.id from AuthAuditEvent ae where ae.createdAt < :cutoff and ae.id > :afterId order by ae.id")
  fun findIdsToPurge(
    @Param("cutoff") cutoff: Date,
    @Param("afterId") afterId: Long,
    pageable: Pageable,
  ): List<Long>

  fun deleteAllByIdIn(ids: Collection<Long>)
}
