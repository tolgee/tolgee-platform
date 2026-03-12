package io.tolgee.repository.qa

import io.tolgee.model.qa.LanguageQaConfig
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface LanguageQaConfigRepository : JpaRepository<LanguageQaConfig, Long> {
  fun findByLanguageProjectIdAndLanguageId(
    projectId: Long,
    languageId: Long,
  ): LanguageQaConfig?

  fun findAllByLanguageProjectId(projectId: Long): List<LanguageQaConfig>

  fun deleteByLanguageProjectIdAndLanguageId(
    projectId: Long,
    languageId: Long,
  )
}
