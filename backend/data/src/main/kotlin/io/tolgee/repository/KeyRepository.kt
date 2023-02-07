package io.tolgee.repository

import io.tolgee.model.key.Key
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KeyRepository : JpaRepository<Key, Long> {

  @Query(
    """
    from Key k
    left join k.namespace ns
    where 
      k.name = :name 
      and k.project.id = :projectId 
      and (ns.name = :namespace or (ns is null and :namespace is null))
  """
  )
  fun getByNameAndNamespace(projectId: Long, name: String, namespace: String?): Optional<Key>

  @Query(
    """
      from Key k left join fetch k.namespace where k.project.id = :projectId
    """
  )
  fun getAllByProjectId(projectId: Long): Set<Key>

  @Query(
    """
      from Key k left join fetch k.namespace where k.project.id = :projectId order by k.id
    """
  )
  fun getAllByProjectIdSortedById(projectId: Long): List<Key>

  @Query("select k.id from Key k where k.project.id = :projectId")
  fun getIdsByProjectId(projectId: Long?): List<Long>
  fun deleteAllByIdIn(ids: Collection<Long>)
  fun findAllByIdIn(ids: Collection<Long>): List<Key>

  @Query("from Key k join fetch k.project left join fetch k.keyMeta where k.id in :ids")
  fun findWithProjectsAndMetas(ids: Set<Long>): List<Key>

  @Query(
    """
      select k from Key k 
      left join fetch k.namespace where k.project.id = :projectId
    """,
    countQuery = """
      select count(k) from Key k 
      where k.project.id = :projectId
    """
  )
  fun getAllByProjectId(projectId: Long, pageable: Pageable): Page<Key>
}
