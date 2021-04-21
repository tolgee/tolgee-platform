package io.tolgee.repository.dataImport.issues

import io.tolgee.model.dataImport.issues.ImportFileIssue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImportFileIssueRepository : JpaRepository<ImportFileIssue, Long>
