package io.tolgee.repository

import io.tolgee.model.key.Namespace
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface NamespaceRepository : JpaRepository<Namespace, Long> {
  fun findByNameAndProjectId(
    name: String,
    projectId: Long,
  ): Namespace?

  @Query(
    """
    select ns.id, count(k.id) from Key k join k.namespace ns on ns.id in :namespaceIds group by ns.id
  """,
  )
  fun getKeysInNamespaceCount(namespaceIds: Collection<Long>): List<Array<Long>>

  fun getAllByProjectId(id: Long): List<Namespace>

  fun getAllByProjectId(
    id: Long,
    pageable: Pageable,
  ): Page<Namespace>

  @Query(
    """
    select count(k) > 0 from Key k where k.namespace is null and k.project.id = :projectId
  """,
  )
  fun isDefaultUsed(projectId: Long): Boolean

  @Query(
    """
    from Namespace n where n.id = :namespaceId and n.project.id = :projectId
  """,
  )
  fun findOneByProjectIdAndId(
    projectId: Long,
    namespaceId: Long,
  ): Namespace?

  @Query(
    """
    from Namespace n where n.name = :name and n.project.id = :projectId
  """,
  )
  fun findOneByProjectIdAndName(
    projectId: Long,
    name: String,
  ): Namespace?

  @Modifying
  @Query(
    """
    delete from namespace where project_id = :projectId
  """,
    nativeQuery = true,
  )
  fun deleteAllByProjectId(projectId: Long)
}
