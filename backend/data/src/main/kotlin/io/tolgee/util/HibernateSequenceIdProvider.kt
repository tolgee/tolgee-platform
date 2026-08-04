package io.tolgee.util

import io.tolgee.model.ALLOCATION_SIZE
import io.tolgee.model.SEQUENCE_NAME
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * Hands out ids from the same pooled blocks Hibernate uses, so rows written by native statements
 * cannot collide with entities persisted through JPA.
 */
@Component
class HibernateSequenceIdProvider(
  private val jdbcTemplate: JdbcTemplate,
) {
  private val sequenceIdProvider = SequenceIdProvider(SEQUENCE_NAME, ALLOCATION_SIZE)

  @Synchronized
  fun next(): Long =
    jdbcTemplate.execute(
      ConnectionCallback { connection -> sequenceIdProvider.next(connection) },
    )!!
}
