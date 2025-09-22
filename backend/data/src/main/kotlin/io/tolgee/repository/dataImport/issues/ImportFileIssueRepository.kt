package io.tolgee.repository.dataImport.issues

import io.tolgee.model.dataImport.issues.ImportFileIssue
import io.tolgee.model.views.ImportFileIssueView
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ImportFileIssueRepository : JpaRepository<ImportFileIssue, Long> {
  @Query("""select ifi from ImportFileIssue ifi where ifi.file.id = :fileId""")
  fun findAllByFileIdView(
    fileId: Long,
    pageable: Pageable,
  ): Page<ImportFileIssueView>
}
