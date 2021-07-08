package io.tolgee.repository

import io.tolgee.model.ApiKey
import io.tolgee.model.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ApiKeyRepository : JpaRepository<ApiKey, Long> {
  fun findByKey(key: String): Optional<ApiKey>
  fun getAllByUserAccountOrderById(userAccount: UserAccount): LinkedHashSet<ApiKey>
  fun getAllByProjectId(projectId: Long): Set<ApiKey>
  fun deleteAllByProjectId(projectId: Long)
}
