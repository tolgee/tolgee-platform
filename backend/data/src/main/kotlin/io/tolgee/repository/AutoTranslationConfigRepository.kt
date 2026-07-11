package io.tolgee.repository

import io.tolgee.model.AutoTranslationConfig
import io.tolgee.model.Project
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AutoTranslationConfigRepository : JpaRepository<AutoTranslationConfig, Long> {
  fun findOneByProjectAndTargetLanguageId(
    project: Project,
    languageId: Long?,
  ): AutoTranslationConfig?

  fun findByProjectAndTargetLanguageIdIn(
    project: Project,
    languageIds: List<Long>,
  ): List<AutoTranslationConfig>

  @Query(
    """
    select atc from AutoTranslationConfig atc
    where atc.project = :project and atc.targetLanguage is null
  """,
  )
  fun findDefaultForProject(project: Project): AutoTranslationConfig?

  fun findAllByProject(project: Project): List<AutoTranslationConfig>?
}
