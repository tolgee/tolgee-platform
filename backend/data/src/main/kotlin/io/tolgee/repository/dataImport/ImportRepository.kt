package io.tolgee.repository.dataImport

import io.tolgee.model.dataImport.Import
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ImportRepository : JpaRepository<Import, Long> {
  @Query(
    """
    select i from Import i 
    left join i.branch b
    where i.project.id = :projectId 
        and i.author.id = :authorId 
        and i.deletedAt is null
        and ((b.name = :branch and b.deletedAt is null) or (:branch is null and (b is null or b.isDefault))) 
  """,
  )
  fun findByProjectIdAndAuthorId(
    projectId: Long,
    authorId: Long,
    branch: String?,
  ): Import?

  @Query(
    """
    select i from Import i where i.project.id = :projectId and i.deletedAt is null
  """,
  )
  fun findAllByProjectId(projectId: Long): List<Import>

  @Query(
    """
    select distinct if.namespace from ImportFile if where if.import.id = :importId
  """,
  )
  fun getAllNamespaces(importId: Long): Set<String?>

  @Query(
    """
    from Import i where i.id = :importId and i.deletedAt is not null
  """,
  )
  fun findDeleted(importId: Long): Import?
}
