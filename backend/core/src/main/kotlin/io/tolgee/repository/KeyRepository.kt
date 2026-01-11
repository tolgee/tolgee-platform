package io.tolgee.repository

import io.tolgee.dtos.queryResults.KeyView
import io.tolgee.dtos.queryResults.keyDisabledLanguages.KeyDisabledLanguagesQueryResultView
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.service.key.KeySearchResultView
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
@Lazy
interface KeyRepository : JpaRepository<Key, Long> {
  @Query(
    """
    from Key k
    left join k.namespace ns
    where 
      k.name = :name 
      and k.project.id = :projectId 
      and (ns.name = :namespace or (ns is null and :namespace is null))
  """,
  )
  fun getByNameAndNamespace(
    projectId: Long,
    name: String,
    namespace: String?,
  ): Optional<Key>

  @Query(
    """
      from Key k left join fetch k.namespace left join fetch k.keyMeta where k.project.id = :projectId
    """,
  )
  fun getAllByProjectId(projectId: Long): Set<Key>

  @Query(
    """
     select new io.tolgee.dtos.queryResults.KeyView(k.id, k.name, ns.name, km.description, km.custom)
     from Key k
     left join k.keyMeta km
     left join k.namespace ns
     where k.project.id = :projectId order by k.id
    """,
  )
  fun getAllByProjectIdSortedById(projectId: Long): List<KeyView>

  @Query("select k from Key k left join fetch k.keyMeta km where k.project.id = :projectId")
  fun getByProjectIdWithFetchedMetas(projectId: Long?): List<Key>

  fun deleteAllByIdIn(ids: Collection<Long>)

  fun findAllByProjectIdAndIdIn(
    projectId: Long,
    ids: Collection<Long>,
  ): List<Key>

  fun findAllByIdIn(ids: Collection<Long>): List<Key>

  @Query(
    """
      from Key k 
      left join fetch k.namespace 
      left join fetch k.keyMeta 
      left join fetch k.keyScreenshotReferences 
      where k.id in :ids
    """,
  )
  fun findAllByIdInForDelete(ids: Collection<Long>): List<Key>

  @Query("from Key k join fetch k.project left join fetch k.keyMeta where k.id in :ids")
  fun findWithProjectsAndMetas(ids: Set<Long>): List<Key>

  @Query(
    """
    select k.id as id, ns.name as namespace, km.description as description, 
        k.name as name, bt.text as baseTranslation, t.text as translation 
        from key k
       join project p on p.id = k.project_id and p.id = :projectId
       left join namespace ns on k.namespace_id = ns.id
       left join key_meta km on k.id = km.key_id
       left join language l on p.id = l.project_id and l.tag = :languageTag
       left join translation bt on bt.key_id = k.id and (bt.language_id = p.base_language_id)
       left join translation t on t.key_id = k.id and (t.language_id = l.id),
    lower(f_unaccent(:search)) as searchUnaccent
    where (
          lower(f_unaccent(ns.name)) %> searchUnaccent
          or lower(f_unaccent(k.name)) %> searchUnaccent
          or lower(f_unaccent(t.text)) %> searchUnaccent
          or lower(f_unaccent(bt.text)) %> searchUnaccent
          )
       order by 
       (
       3 * (ns.name <-> searchUnaccent) + 
       3 * (k.name <-> searchUnaccent) + 
       (t.text <-> searchUnaccent) +
       (bt.text <-> searchUnaccent)
       ) desc, k.id
    limit :#{#pageable.pageSize}
    offset :#{#pageable.offset}
  """,
    nativeQuery = true,
    countQuery = """
      select count(k.id) 
      from key k
       join project p on p.id = k.project_id and p.id = :projectId
       left join namespace ns on k.namespace_id = ns.id
       left join language l on p.id = l.project_id and l.tag = :languageTag
       left join translation bt on bt.key_id = k.id and (bt.language_id = p.base_language_id)
       left join translation t on t.key_id = k.id and (t.language_id = l.id),
      lower(f_unaccent(:search)) as searchUnaccent  
      where (
          lower(f_unaccent(ns.name)) %> searchUnaccent
          or lower(f_unaccent(k.name)) %> searchUnaccent
          or lower(f_unaccent(t.text)) %> searchUnaccent
          or lower(f_unaccent(bt.text)) %> searchUnaccent
          )
      """,
  )
  fun searchKeys(
    search: String,
    projectId: Long,
    languageTag: String?,
    pageable: Pageable,
  ): Page<KeySearchResultView>

  @Query(
    """
     select new io.tolgee.dtos.queryResults.KeyView(k.id, k.name, ns.name, km.description, km.custom)
     from Key k
     left join k.keyMeta km
     left join k.namespace ns
     where k.project.id = :projectId
    """,
    countQuery = """
      select count(k) from Key k 
      where k.project.id = :projectId
    """,
  )
  fun getAllByProjectId(
    projectId: Long,
    pageable: Pageable,
  ): Page<KeyView>

  @Query(
    """
     select new io.tolgee.dtos.queryResults.KeyView(k.id, k.name, ns.name, km.description, km.custom)
     from Key k
     left join k.keyMeta km
     left join k.namespace ns
     where k.project.id = :projectId
     and k.id = :id
    """,
  )
  fun findView(
    projectId: Long,
    id: Long,
  ): KeyView?

  @Query(
    """
    select k from Key k
    left join fetch k.keyMeta km
    left join fetch km.tags
    where k in :keys
  """,
  )
  fun getWithTags(keys: Set<Key>): List<Key>

  @Query(
    """
    select k from Key k
    left join fetch k.keyMeta km
    left join fetch km.tags
    where k.id in :keyIds and k.project.id = :projectId
  """,
  )
  fun getWithTagsByIds(
    projectId: Long,
    keyIds: Iterable<Long>,
  ): Set<Key>

  @Query(
    """
    select k.project.id from Key k where k.id in :keysIds
  """,
  )
  fun getProjectIdsForKeyIds(keysIds: List<Long>): List<Long>

  @Query(
    """
    select k from Key k
    left join fetch k.namespace
    where k.id in :keyIds
  """,
  )
  fun getKeysWithNamespaces(keyIds: List<Long>): List<Key>

  @Query(
    """
    from Language l
    join l.translations t
    where t.key.id = :keyId
      and l.project.id = :projectId
      and t.state = io.tolgee.model.enums.TranslationState.DISABLED
   order by l.id
  """,
  )
  fun getDisabledLanguages(
    projectId: Long,
    keyId: Long,
  ): List<Language>

  @Query(
    """
    select new io.tolgee.dtos.queryResults.keyDisabledLanguages.KeyDisabledLanguagesQueryResultView(
        k.id, k.name, ns.name, l.id, l.tag
    )
    from Key k
    left join k.namespace ns
    join k.translations t on t.state = io.tolgee.model.enums.TranslationState.DISABLED
    join t.language l
    where k.project.id = :projectId
    order by k.id, l.id
  """,
  )
  fun getDisabledLanguages(projectId: Long): List<KeyDisabledLanguagesQueryResultView>

  fun findByProjectIdAndId(
    projectId: Long,
    keyId: Long,
  ): Key?

  @Query(
    """
     select new io.tolgee.dtos.queryResults.KeyView(k.id, k.name, ns.name, km.description, km.custom)
     from Key k
     left join k.keyMeta km
     left join k.namespace ns
      where k.id in :ids
  """,
  )
  fun getViewsByKeyIds(ids: List<Long>): List<KeyView>

  @Query(
    """
    select count(k) from Key k
    join k.project p on p.deletedAt is null
    join p.organizationOwner o on o.deletedAt is null
  """,
  )
  fun countAllOnInstance(): Long
}
