package io.tolgee.repository

import io.tolgee.model.QuickStart
import io.tolgee.model.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface QuickStartRepository : JpaRepository<QuickStart, Long> {
  fun findByUserAccount(userAccount: UserAccount): QuickStart?
}
