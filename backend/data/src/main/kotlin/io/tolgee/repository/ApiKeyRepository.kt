package io.tolgee.repository

import io.tolgee.model.ApiKey
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.Optional

@Repository
@Lazy
interface ApiKeyRepository : JpaRepository<ApiKey, Long> {
  fun findByKeyHash(hash: String): Optional<ApiKey>

  fun getAllByUserAccountIdOrderById(userAccountId: Long): LinkedHashSet<ApiKey>

  fun getAllByProjectId(projectId: Long): Set<ApiKey>

  fun getAllByProjectId(
    projectId: Long,
    pageable: Pageable,
  ): Page<ApiKey>

  fun deleteAllByProjectId(projectId: Long)

  @EntityGraph(attributePaths = ["project", "userAccount", "scopesEnum"])
  @Query(
    """select ak from ApiKey ak
    left join ak.project p 
    left join ak.userAccount u
    where u.id = :userAccountId 
    and (p.id = :filterProjectId or :filterProjectId is null)
    and p.deletedAt is null
  """,
  )
  fun getAllByUserAccount(
    userAccountId: Long,
    filterProjectId: Long?,
    pageable: Pageable,
  ): Page<ApiKey>

  @Modifying
  @Query("UPDATE ApiKey ak SET ak.lastUsedAt = ?2 WHERE ak.id = ?1")
  fun updateLastUsedById(
    id: Long,
    lastUsed: Date,
  )
}
