package io.tolgee.repository.machineTranslation

import io.tolgee.model.mtServiceConfig.MtServiceConfig
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface MtServiceConfigRepository : JpaRepository<MtServiceConfig, Long> {
  @Query(
    """
    select ptsc from MtServiceConfig ptsc
    join Language l on l.id = :languageId 
        and (
            ptsc.targetLanguage.id = :languageId 
            or (ptsc.targetLanguage.id is null and ptsc.project.id = l.project.id)
            )
  """,
  )
  fun findAllByTargetLanguageId(languageId: Long): List<MtServiceConfig>

  fun findAllByProjectId(projectId: Long): List<MtServiceConfig>

  fun deleteAllByTargetLanguageId(targetLanguageId: Long)

  fun deleteAllByProjectId(projectId: Long)

  @Query(
    """
    select ptsc from MtServiceConfig ptsc
    where ptsc.targetLanguage.id in :languageIds 
        or (ptsc.targetLanguage.id is null and ptsc.project.id = :projectId)
  """,
  )
  fun findAllByTargetLanguageIdIn(
    languageIds: List<Long>,
    projectId: Long,
  ): List<MtServiceConfig>
}
