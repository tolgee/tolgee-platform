package io.tolgee.repository

import io.tolgee.jobs.migration.translationStats.StatsMigrationTranslationView
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.SimpleTranslationView
import io.tolgee.model.views.TranslationMemoryItemView
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TranslationRepository : JpaRepository<Translation, Long> {
  @Query(
    """select t.text as text, l.tag as languageTag, k.name as key 
        from Translation t 
        join t.key k
        left join k.namespace n
        join t.language l 
        where t.key.project.id = :projectId 
         and l.tag in :languages
         and ((n.name is null and :namespace is null) or n.name = :namespace)
        order by k.name
   """
  )
  fun getTranslations(languages: Set<String>, namespace: String?, projectId: Long): List<SimpleTranslationView>

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

  @Query(
    """from Translation t 
    join fetch t.key k 
    left join fetch k.keyMeta 
    left join fetch t.comments
    where t.key.id in :keyIds"""
  )
  fun getAllByKeyIdIn(keyIds: Collection<Long>): Collection<Translation>

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
      select target.text as targetTranslationText, baseTranslation.text as baseTranslationText, key.name as keyName, key.id as keyId, 
      similarity(baseTranslation.text, :baseTranslationText) as similarity
      from Translation baseTranslation
      join baseTranslation.key key
      join Translation target on 
            target.key = key and 
            target.language = :targetLanguage and
            target.text <> '' and
            target.text is not null
      where baseTranslation.language = :baseLanguage and
        cast(similarity(baseTranslation.text, :baseTranslationText) as float)> 0.5F and
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
      select target.text as targetTranslationText, baseTranslation.text as baseTranslationText, key.name as keyName, 
      1 as similarity
      from Translation baseTranslation
      join baseTranslation.key key
      join Translation target on 
            target.key = key and 
            target.language = :targetLanguage and
            target.text <> '' and
            target.text is not null
      where baseTranslation.language = :baseLanguage and
        baseTranslation.text = :baseTranslationText and
        key <> :key
      """
  )
  fun getTranslationMemoryValue(
    baseTranslationText: String,
    key: Key,
    baseLanguage: Language,
    targetLanguage: Language,
    pageable: Pageable = PageRequest.of(0, 1)
  ): List<TranslationMemoryItemView>

  @Query(
    """
    select t.id as id, t.text as text, t.language.tag as languageTag
    from Translation t
  """
  )
  fun findAllForStatsUpdate(pageable: Pageable): Page<StatsMigrationTranslationView>

  @Query(
    """
    select t.id
    from Translation t
    where t.text <> null and (t.wordCount is null or t.characterCount is null or (length(text) <> 0 and t.characterCount = 0))
    order by t.id
  """
  )
  fun findAllIdsForStatsUpdate(): List<Long>

  @Query(
    """
    update Translation t
    set t.outdated = true
    where t.id in (
      select t.id
      from Translation t
      join t.key k on k.id in :keyIds
      join k.project p
      where t.language.id <> p.baseLanguage.id
    )
  """
  )
  @Modifying
  fun setOutdated(keyIds: List<Long>)

  @Query(
    """
    from Translation t
    join t.language l on l.tag in :languageTags
    where t.key.id in :keys
  """
  )
  fun getForKeys(keys: List<Long>, languageTags: List<String>): List<Translation>

  fun findAllByKeyIdInAndLanguageIdIn(keysIds: List<Long>, languagesIds: List<Long>): List<Translation>
  fun getAllByKeyIdInAndLanguageIdIn(keyIds: List<Long>, languageIds: List<Long>): List<Translation>

  @Query(
    """
    from Translation t
    join t.key k
    where k.project.id = :projectId
  """
  )
  fun getAllByProjectId(projectId: Long): List<Translation>
  @Query("""
    from Translation t
    where t.key = :key and t.language.tag in :languageTags
  """)
  fun findForKeyByLanguages(key: Key, languageTags: Collection<String>): List<Translation>
}
