package io.tolgee.repository

import io.tolgee.model.Pat
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PatRepository : JpaRepository<Pat, Long> {
  fun findByTokenHash(tokenHash: String): Pat?

  fun findAllByUserAccountId(userId: Long, pageable: Pageable): Page<Pat>
}
