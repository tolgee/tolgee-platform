package io.tolgee.repository.dataImport.issues

import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.issues.ImportFileIssue
import io.tolgee.model.views.ImportFileIssueView
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Lazy
interface ImportFileIssueRepository : JpaRepository<ImportFileIssue, Long> {
  @Query("""select ifi from ImportFileIssue ifi where ifi.file.id = :fileId""")
  fun findAllByFileIdView(
    fileId: Long,
    pageable: Pageable,
  ): Page<ImportFileIssueView>

  @Transactional
  @Query(
    """delete from ImportFileIssue ifi where ifi.file in 
        (select f from ImportFile f where f.import = :import)""",
  )
  @Modifying
  fun deleteAllByImport(import: Import)
}
