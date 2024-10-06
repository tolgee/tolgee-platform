package io.tolgee.repository.dataImport.issues

import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.issues.ImportFileIssueParam
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Lazy
interface ImportFileIssueParamRepository : JpaRepository<ImportFileIssueParam, Long> {
  @Transactional
  @Modifying
  @Query(
    """delete from ImportFileIssueParam ifip where ifip.issue in 
        (select ifi from ifip.issue ifi join ifi.file if where if.import = :import)
        """,
  )
  fun deleteAllByImport(import: Import)
}
