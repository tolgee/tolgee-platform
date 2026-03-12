package io.tolgee.repository.qa

import io.tolgee.model.qa.LanguageQaConfig
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface LanguageQaConfigRepository : JpaRepository<LanguageQaConfig, Long> {
  fun findByProjectIdAndLanguageId(
    projectId: Long,
    languageId: Long,
  ): LanguageQaConfig?

  fun findAllByProjectId(projectId: Long): List<LanguageQaConfig>

  fun deleteByProjectIdAndLanguageId(
    projectId: Long,
    languageId: Long,
  )
}
