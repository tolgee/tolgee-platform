package io.tolgee.repository.dataImport.issues

import io.tolgee.model.dataImport.issues.ImportFileIssueParam
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ImportFileIssueParamRepository : JpaRepository<ImportFileIssueParam, Long>
