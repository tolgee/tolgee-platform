package io.tolgee.repository

import io.tolgee.model.ApiKey
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ApiKeyRepository : JpaRepository<ApiKey, Long> {
  fun findByKey(key: String): Optional<ApiKey>
  fun getAllByUserAccountIdOrderById(userAccountId: Long): LinkedHashSet<ApiKey>
  fun getAllByProjectId(projectId: Long): Set<ApiKey>
  fun getAllByProjectId(projectId: Long, pageable: Pageable): Page<ApiKey>
  fun deleteAllByProjectId(projectId: Long)

  @EntityGraph(attributePaths = ["project", "userAccount", "scopesEnum"])
  @Query(
    """select ak from ApiKey ak
    left join ak.project p 
    left join ak.userAccount u
    where u.id = :userAccountId 
    and (p.id = :filterProjectId or :filterProjectId is null)
  """
  )
  fun getAllByUserAccount(userAccountId: Long, filterProjectId: Long?, pageable: Pageable): Page<ApiKey>
}
