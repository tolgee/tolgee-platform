package io.tolgee.repository

import io.tolgee.model.Pat
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
@Lazy
interface PatRepository : JpaRepository<Pat, Long> {
  fun findByTokenHash(tokenHash: String): Pat?

  fun findAllByUserAccountId(
    userId: Long,
    pageable: Pageable,
  ): Page<Pat>

  @Modifying
  @Query("UPDATE Pat p SET p.lastUsedAt = ?2 WHERE p.id = ?1")
  fun updateLastUsedById(
    id: Long,
    lastUsed: Date,
  )
}
