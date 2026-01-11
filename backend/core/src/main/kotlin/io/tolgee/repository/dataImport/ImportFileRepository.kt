package io.tolgee.repository.dataImport

import io.tolgee.model.dataImport.ImportFile
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ImportFileRepository : JpaRepository<ImportFile, Long> {
  @Query(
    """
    select f from ImportFile f
    where f.import.project.id = :projectId 
    and f.import.author.id = :authorId
    and f.id = :id
  """,
  )
  fun finByProjectAuthorAndId(
    projectId: Long,
    authorId: Long,
    id: Long,
  ): ImportFile?
}
