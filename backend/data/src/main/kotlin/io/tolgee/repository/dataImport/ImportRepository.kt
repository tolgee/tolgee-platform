package io.tolgee.repository.dataImport

import io.tolgee.model.dataImport.Import
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ImportRepository : JpaRepository<Import, Long> {
  fun findByProjectIdAndAuthorId(projectId: Long, authorId: Long): Import?

  fun findAllByProjectId(projectId: Long): List<Import>

  @Query(
    """
    select distinct if.namespace from ImportFile if where if.import.id = :importId
  """
  )
  fun getAllNamespaces(importId: Long): Set<String?>
}
