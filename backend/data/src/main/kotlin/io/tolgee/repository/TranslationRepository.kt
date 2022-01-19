package io.tolgee.repository

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.dtos.request.export.ExportParamsNull
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.SimpleTranslationView
import io.tolgee.model.views.TranslationMemoryItemView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TranslationRepository : JpaRepository<Translation, Long> {
  @Query(
    """select t.text as text, l.tag as languageTag, k.name as key from Translation t 
        join t.key k join t.language l where t.key.project.id = :projectId and l.tag in :languages"""
  )
  fun getTranslations(languages: Set<String>, projectId: Long): List<SimpleTranslationView>

  @Query(
    "from Translation t " +
      "join fetch Key k on t.key = k " +
      "where k = :key and k.project = :project and t.language in :languages"
  )
  fun getTranslations(key: Key, project: Project, languages: Collection<Language>): Set<Translation>
  fun findOneByKeyAndLanguage(key: Key, language: Language): Optional<Translation>
  fun findOneByKeyIdAndLanguageId(key: Long, language: Long): Translation?

  @Query(
    """
        from Translation t join fetch t.key k left join fetch k.keyMeta where t.language.id = :languageId
    """
  )
  fun getAllByLanguageId(languageId: Long): List<Translation>
  fun getAllByKeyIdIn(keyIds: Iterable<Long>): Collection<Translation>

  @Query(
    """select t.id from Translation t where t.key.id in 
        (select k.id from t.key k where k.project.id = :projectId)"""
  )
  fun selectIdsByProject(projectId: Long): List<Long>

  fun deleteByIdIn(ids: Collection<Long>)

  /**
   * inputKey param is optional. When provided, target translation for given won't be returned,
   * because it's the base translation
   */
  @Query(
    """
      select target.text as targetTranslationText, baseTranslation.text as baseTranslationText, key.name as keyName, 
      similarity(baseTranslation.text, :baseTranslationText) as similarity
      from Translation baseTranslation
      join baseTranslation.key key
      join Translation target on 
            target.key = key and 
            target.language = :targetLanguage and
            target.text <> '' and
            target.text is not null
      where baseTranslation.language = :baseLanguage and
        similarity(baseTranslation.text, :baseTranslationText) > 0.5 and
        (:key is null or key <> :key)
      order by similarity desc
      """
  )
  fun getTranslateMemorySuggestions(
    baseTranslationText: String,
    key: Key? = null,
    baseLanguage: Language,
    targetLanguage: Language,
    pageable: Pageable
  ): Page<TranslationMemoryItemView>

  @Query(
    """
    from Translation t
    join fetch t.key k
    join fetch k.project p
    join fetch t.language l
    left join fetch k.keyMeta km
    left join fetch km.tags tg
    where p.id = :projectId
    and ((:#{#pnn.languages}) = true or l.tag in (:#{#params.languages}))
    and ((:#{#pnn.filterKeyId}) = true or k.id in (:#{#params.filterKeyId}))
    and ((:#{#pnn.filterKeyIdNot}) = true or k.id not in (:#{#params.filterKeyIdNot}))
    and ((:#{#pnn.filterTag}) = true  or tg.name in (:#{#params.filterTag}))
    and ((:#{#pnn.filterKeyPrefix}) = true or k.name like concat(cast(:#{#params.filterKeyPrefix} as text), '%'))
    and ((:#{#pnn.filterState}) = true or t.state in (:#{#params.filterState}))
    and ((:#{#pnn.filterStateNot}) = true or t.state not in (:#{#params.filterStateNot}))
    order by k.name
  """
  )
  fun getDataForExport(
    projectId: Long,
    @Param("params") params: ExportParams,
    @Param("pnn") pnn: ExportParamsNull
  ): List<Translation>
}
