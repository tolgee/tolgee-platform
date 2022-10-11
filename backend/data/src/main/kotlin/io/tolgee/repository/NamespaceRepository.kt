package io.tolgee.repository

import io.tolgee.model.key.Namespace
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NamespaceRepository : JpaRepository<Namespace, Long> {
  fun findByNameAndProjectId(name: String, projectId: Long): Namespace?

  @Query(
    """
    select ns.id, count(k.id) from Key k join k.namespace ns on ns.id in :namespaceIds group by ns.id
  """
  )
  fun getKeysInNamespaceCount(namespaceIds: List<Long>): List<Array<Long>>
  fun getAllByProjectId(id: Long): List<Namespace>
}
