package io.tolgee.repository

import io.tolgee.jobs.migration.translationStats.StatsMigrationTranslationView
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.SimpleTranslationView
import io.tolgee.model.views.TranslationMemoryItemView
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.Optional

@Repository
@Lazy
interface TranslationRepository : JpaRepository<Translation, Long> {
  @Query(
    """select t.text as text, l.tag as languageTag, k.name as key
        from Translation t
        join t.key k
        left join k.namespace n
        left join k.keyMeta km
        left join km.tags kmt
        join t.language l
        where t.key.project.id = :projectId
         and l.tag in :languages
         and ((n.name is null and :namespace is null) or n.name = :namespace)
         and (:filterTags is null or kmt.name in :filterTags)
        group by t.id, l.tag, k.name
        order by k.name
   """,
  )
  fun getTranslations(
    languages: Set<String>,
    namespace: String?,
    projectId: Long,
    filterTags: List<String>?,
  ): List<SimpleTranslationView>

  @Query(
    "from Translation t " +
      "join fetch Key k on t.key = k " +
      "where k = :key and k.project = :project and t.language.id in :languageIds",
  )
  fun getTranslations(
    key: Key,
    project: Project,
    languageIds: Collection<Long>,
  ): Set<Translation>

  fun findOneByKeyAndLanguageId(
    key: Key,
    languageId: Long,
  ): Optional<Translation>

  @Query(
    """
    from Translation t 
    join fetch t.key k
    where t.key.id = :keyId and k.project.id = :projectId and t.language.id = :languageId
  """,
  )
  fun findOneByProjectIdAndKeyIdAndLanguageId(
    projectId: Long,
    keyId: Long,
    languageId: Long,
  ): Translation?

  @Query(
    """
        from Translation t join fetch t.key k left join fetch k.keyMeta where t.language.id = :languageId
    """,
  )
  fun getAllByLanguageId(languageId: Long): List<Translation>

  @Query(
    """from Translation t 
    join fetch t.key k 
    left join fetch k.keyMeta 
    left join fetch t.comments
    where t.key.id in :keyIds""",
  )
  fun getAllByKeyIdIn(keyIds: Collection<Long>): Collection<Translation>

  @Query(
    """from Translation t 
    join fetch t.key k 
    left join fetch k.keyMeta 
    left join fetch t.comments
    where t.key.id in :keyIds
    and (:excludeTranslationIds is null or t.id not in :excludeTranslationIds)
    """,
  )
  fun getAllByKeyIdInExcluding(
    keyIds: Collection<Long>,
    excludeTranslationIds: List<Long>? = null,
  ): Collection<Translation>

  fun deleteByIdIn(ids: Collection<Long>)

  @Query(
    """
      select 
      new io.tolgee.model.views.TranslationMemoryItemView(baseTranslation.text, target.text, k.name, null, 1, k.id)
      from Translation baseTranslation
      join baseTranslation.key k
      join k.project p
      join Translation target on
            target.key = k and
            target.language.id = :targetLanguageId and
            target.text <> '' and
            target.text is not null
      where baseTranslation.language = p.baseLanguage and
        baseTranslation.text = :baseTranslationText and
        k <> :key
      """,
  )
  fun getTranslationMemoryValue(
    baseTranslationText: String,
    key: Key,
    targetLanguageId: Long,
    pageable: Pageable = Pageable.ofSize(1),
  ): List<TranslationMemoryItemView>

  @Query(
    """
    select t.id as id, t.text as text, t.language.tag as languageTag
    from Translation t
  """,
  )
  fun findAllForStatsUpdate(pageable: Pageable): Page<StatsMigrationTranslationView>

  @Query(
    """
    select t.id
    from Translation t
    where t.text is not null and (t.wordCount is null or t.characterCount is null or (length(text) <> 0 and t.characterCount = 0))
    order by t.id
  """,
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
  """,
  )
  @Modifying
  fun setOutdated(keyIds: List<Long>)

  @Query(
    """
    from Translation t
    join t.language l on l.tag in :languageTags
    where t.key.id in :keys
  """,
  )
  fun getForKeys(
    keys: List<Long>,
    languageTags: List<String>,
  ): List<Translation>

  fun findAllByKeyIdInAndLanguageIdIn(
    keysIds: List<Long>,
    languagesIds: List<Long>,
  ): List<Translation>

  fun getAllByKeyIdInAndLanguageIdIn(
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): List<Translation>

  @Query(
    """
    from Translation t
    join t.key k
    where k.project.id = :projectId
  """,
  )
  fun getAllByProjectId(projectId: Long): List<Translation>

  @Query(
    """
    from Translation t
    where t.key = :key and t.language.tag in :languageTags and t.language.deletedAt is null
  """,
  )
  fun findForKeyByLanguages(
    key: Key,
    languageTags: Collection<String>,
  ): List<Translation>

  @Query(
    """
    from Translation t where t.key.project.id = :projectId and t.id = :translationId
  """,
  )
  fun find(
    projectId: Long,
    translationId: Long,
  ): Translation?

  @Query("select max(coalesce(t.updatedAt, t.createdAt)) from Translation t where t.language.id = :languageId")
  fun getLastModifiedDate(languageId: Long): Date?

  @Query(
    """
    SELECT t FROM Translation t
    LEFT JOIN FETCH t.labels 
    WHERE t.key.id IN :keyIds AND t.language.id IN :languageIds
    """,
  )
  fun getTranslationsWithLabels(
    keyIds: Collection<Long>,
    languageIds: Collection<Long>,
  ): List<Translation>
}
