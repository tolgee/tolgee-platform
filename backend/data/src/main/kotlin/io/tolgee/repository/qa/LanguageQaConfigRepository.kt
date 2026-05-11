package io.tolgee.repository.qa

import io.tolgee.model.qa.LanguageQaConfig
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
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

  @Modifying
  @Query(
    nativeQuery = true,
    value =
      "DELETE FROM language_qa_config WHERE language_id IN " +
        "(SELECT id FROM language WHERE project_id = :projectId)",
  )
  fun deleteAllByProjectId(projectId: Long)

  @Modifying
  @Query("DELETE FROM LanguageQaConfig c WHERE c.language.id = :languageId")
  fun deleteAllByLanguageId(languageId: Long)
}
