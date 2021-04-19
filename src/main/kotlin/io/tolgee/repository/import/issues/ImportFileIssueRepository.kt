package io.tolgee.repository.import.issues

import io.tolgee.model.import.issues.ImportFileIssue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImportFileIssueRepository : JpaRepository<ImportFileIssue, Long>
