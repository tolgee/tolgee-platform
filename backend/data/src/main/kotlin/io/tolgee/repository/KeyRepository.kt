package io.tolgee.repository

import io.tolgee.dtos.queryResults.KeyView
import io.tolgee.dtos.queryResults.keyDisabledLanguages.KeyDisabledLanguagesQueryResultView
import io.tolgee.model.Language
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import io.tolgee.service.key.KeySearchResultView
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.Optional

@Repository
@Lazy
interface KeyRepository : JpaRepository<Key, Long> {
  @Query(
    value = """
      select count(k.id) from key k
      where k.project_id = :projectId and k.branch_id = :branchId and k.deleted_at is null
    """,
    nativeQuery = true,
  )
  fun countByProjectAndBranch(
    projectId: Long,
    branchId: Long,
  ): Long

  @Query(
    value = """
      select count(k.id) from key k
      where k.project_id = :projectId
      and (k.branch_id = :branchId or (:includeOrphanDefault = true and k.branch_id is null))
      and k.deleted_at is null
    """,
    nativeQuery = true,
  )
  fun countByProjectAndBranchIncludingOrphan(
    projectId: Long,
    branchId: Long,
    includeOrphanDefault: Boolean,
  ): Long

  @Query(
    """
    select k.id from Key k
    where k.project.id = :projectId and k.branch.id = :branchId
    """,
  )
  fun findIdsByProjectAndBranch(
    projectId: Long,
    branchId: Long,
    pageable: Pageable,
  ): Page<Long>

  @Query(
    """
    from Key k
    left join k.namespace ns
    left join k.branch br
    where
      k.name = :name
      and k.project.id = :projectId
      and (ns.name = :namespace or (ns is null and :namespace is null))
      and ((br.name = :branch and br.deletedAt is null) or (:branch is null and (br is null or br.isDefault)))
      and k.deletedAt is null
  """,
  )
  fun getByNameAndNamespace(
    projectId: Long,
    name: String,
    namespace: String?,
    branch: String?,
  ): Optional<Key>

  @Query(
    """
    from Key k
      left join fetch k.namespace
      left join fetch k.branch b
    where k.project.id = :projectId
      and k.name in :names
      and k.deletedAt is null
  """,
  )
  fun findActiveByProjectIdAndNames(
    projectId: Long,
    names: Collection<String>,
  ): List<Key>

  @Query(
    """
      from Key k left join fetch k.namespace left join fetch k.keyMeta where k.project.id = :projectId and k.deletedAt is null
    """,
  )
  fun getAllByProjectId(projectId: Long): Set<Key>

  @Query(
    """
      from Key k
        left join k.branch b
        left join fetch k.namespace
        left join fetch k.keyMeta
    where k.project.id = :projectId
    and ((b.name = :branch and b.deletedAt is null) or (:branch is null and (b is null or b.isDefault)))
    and k.deletedAt is null
    """,
  )
  fun getAllByProjectIdAndBranch(
    projectId: Long,
    branch: String?,
  ): Set<Key>

  @Query(
    """
     select new io.tolgee.dtos.queryResults.KeyView(k.id, k.name, ns.name, km.description, km.custom, k.branch.name, k.maxCharLimit)
     from Key k
     left join k.keyMeta km
     left join k.namespace ns
     left join k.branch br
     where k.project.id = :projectId
        and ((br.name = :branch and br.deletedAt is null) or (:branch is null and (br is null or br.isDefault)))
        and k.deletedAt is null
     order by k.id
    """,
  )
  fun getAllByProjectIdSortedById(
    projectId: Long,
    branch: String?,
  ): List<KeyView>

  @Query("select k from Key k left join fetch k.keyMeta km where k.project.id = :projectId and k.deletedAt is null")
  fun getByProjectIdWithFetchedMetas(projectId: Long?): List<Key>

  fun deleteAllByIdIn(ids: Collection<Long>)

  fun findAllByProjectIdAndIdIn(
    projectId: Long,
    ids: Collection<Long>,
  ): List<Key>

  @Query(
    """
    select b.id
    from Key k
    left join k.branch b
    where k.id in :ids
  """,
  )
  fun getBranchIdsByIds(ids: Collection<Long>): List<Long?>

  fun findAllByIdIn(ids: Collection<Long>): List<Key>

  @Query(
    """
    select distinct k from Key k
    left join fetch k.keyMeta km
    left join fetch k.translations t
    left join fetch t.language lang
    left join fetch k.namespace ns
    where k.id in :ids
    """,
  )
  fun findAllDetailedByIdIn(ids: Collection<Long>): List<Key>

  @Query(
    """
      select distinct k from Key k
      left join fetch k.keyScreenshotReferences ksr
      left join fetch ksr.screenshot s
      where k.id in :ids
    """,
  )
  fun findAllWithScreenshotsByIdIn(ids: Collection<Long>): List<Key>

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
        k.name as name, bt.text as baseTranslation, t.text as translation,
        k.deleted_at as deletedAt, k.is_plural as plural,
        dbu.id as deletedByUserId, dbu.username as deletedByUserUsername,
        dbu.name as deletedByUserName, dbu.avatar_hash as deletedByUserAvatarHash,
        case when dbu.deleted_at is not null then true else false end as deletedByUserDeleted
        from key k
       join project p on p.id = k.project_id and p.id = :projectId
       left join branch br on k.branch_id = br.id
       left join namespace ns on k.namespace_id = ns.id
       left join key_meta km on k.id = km.key_id
       left join language l on p.id = l.project_id and l.tag = :languageTag
       left join translation bt on bt.key_id = k.id and (bt.language_id = p.base_language_id)
       left join translation t on t.key_id = k.id and (t.language_id = l.id)
       left join user_account dbu on k.deleted_by_id = dbu.id,
    lower(f_unaccent(:search)) as searchUnaccent
    where (
          lower(f_unaccent(ns.name)) %> searchUnaccent
          or lower(f_unaccent(k.name)) %> searchUnaccent
          or lower(f_unaccent(t.text)) %> searchUnaccent
          or lower(f_unaccent(bt.text)) %> searchUnaccent
          ) and (
            (br.name = :branch and br.deleted_at is null)
            or (:branch is null and (br.id is null or br.is_default))
          )
          and ((:trashed = true and k.deleted_at is not null) or (:trashed = false and k.deleted_at is null))
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
       left join branch br on k.branch_id = br.id
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
          ) and (
            (br.name = :branch and br.deleted_at is null)
            or (:branch is null and (br.id is null or br.is_default))
          )
          and ((:trashed = true and k.deleted_at is not null) or (:trashed = false and k.deleted_at is null))
      """,
  )
  fun searchKeys(
    search: String,
    projectId: Long,
    languageTag: String?,
    branch: String?,
    trashed: Boolean = false,
    pageable: Pageable,
  ): Page<KeySearchResultView>

  @Query(
    """
     select new io.tolgee.dtos.queryResults.KeyView(k.id, k.name, ns.name, km.description, km.custom, b.name, k.maxCharLimit)
     from Key k
     left join k.keyMeta km
     left join k.namespace ns
     left join k.branch b
     where k.project.id = :projectId
        and ((b.name = :branch and b.deletedAt is null) or (:branch is null and (b is null or b.isDefault)))
        and k.deletedAt is null
    """,
    countQuery = """
      select count(k) from Key k
      left join k.branch b
      where k.project.id = :projectId
        and ((b.name = :branch and b.deletedAt is null) or (:branch is null and (b is null or b.isDefault)))
        and k.deletedAt is null
    """,
  )
  fun getAllByProjectId(
    projectId: Long,
    branch: String?,
    pageable: Pageable,
  ): Page<KeyView>

  @Query(
    """
     select new io.tolgee.dtos.queryResults.KeyView(k.id, k.name, ns.name, km.description, km.custom, br.name, k.maxCharLimit)
     from Key k
     left join k.keyMeta km
     left join k.namespace ns
     left join k.branch br
     where k.project.id = :projectId
     and k.id = :id
     and k.deletedAt is null
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
    where k.id = :keyId
  """,
  )
  fun findOneWithTags(keyId: Long): Key

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
    select k.project.id from Key k where k.id in :keysIds and k.deletedAt is not null
  """,
  )
  fun getSoftDeletedProjectIdsForKeyIds(keysIds: List<Long>): List<Long>

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
      and k.deletedAt is null
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
     select new io.tolgee.dtos.queryResults.KeyView(k.id, k.name, ns.name, km.description, km.custom, br.name, k.maxCharLimit)
     from Key k
     left join k.keyMeta km
     left join k.namespace ns
     left join k.branch br
      where k.id in :ids
  """,
  )
  fun getViewsByKeyIds(ids: List<Long>): List<KeyView>

  @Query(
    """
    select count(k) from Key k
    join k.project p on p.deletedAt is null
    join p.organizationOwner o on o.deletedAt is null
    where k.deletedAt is null
  """,
  )
  fun countAllOnInstance(): Long

  @Query(
    """
      from Key k
      left join fetch k.translations t
      left join fetch k.keyMeta km
      left join fetch k.namespace ns
      left join fetch k.branch b
      left join fetch t.labels l
      left join fetch km.tags tg
      left join fetch t.comments c
      left join fetch c.author ca
      where k.project.id = :projectId
        and k.name = :keyName
        and ((b.name = :branchName and b.deletedAt is null) or (:branchName is null and (b is null or b.isDefault)))
        and k.deletedAt is null
    """,
  )
  fun findPrefetchedByNameAndBranch(
    projectId: Long,
    keyName: String,
    branchName: String?,
  ): Key?

  @Query(
    """
      from Key k
      left join fetch k.branch
      where k.id = :keyId
    """,
  )
  fun findByIdWithBranch(keyId: Long): Key?

  @Query(
    """
      from Key k
      join fetch k.branch b
      where b.id = :branchId
    """,
  )
  fun findAllByBranchId(branchId: Long): List<Key>

  @Query(
    """
    select distinct k from Key k
    left join fetch k.keyMeta km
    left join fetch km.tags
    left join fetch k.translations t
    left join fetch t.labels l
    left join fetch t.language lang
    left join fetch k.namespace ns
    left join fetch k.branch b
    where k.project.id = :projectId and (
      (:includeOrphanDefault = true and (b.id = :branchId or b is null))
      or (:includeOrphanDefault = false and b.id = :branchId)
    )
    and k.deletedAt is null
    """,
  )
  fun findAllDetailedByBranch(
    projectId: Long,
    branchId: Long,
    includeOrphanDefault: Boolean,
  ): List<Key>

  @Query(
    """
    select k from Key k
    left join fetch k.namespace ns
    left join fetch k.branch b
    where k.project.id = :projectId and (
      (:includeOrphanDefault = true and (b.id = :branchId or b is null))
      or (:includeOrphanDefault = false and b.id = :branchId)
    )
    and k.deletedAt is null
    """,
  )
  fun findAllFetchBranchAndNamespace(
    projectId: Long,
    branchId: Long,
    includeOrphanDefault: Boolean,
  ): List<Key>

  @Query(
    """
    select distinct ua from Key k
    join k.deletedBy ua
    left join k.branch br
    where k.project.id = :projectId
      and k.deletedAt is not null
      and ((br.name = :branch and br.deletedAt is null)
           or (:branch is null and (br is null or br.isDefault)))
    order by ua.name, ua.id
    """,
  )
  fun findDistinctDeleters(
    projectId: Long,
    branch: String?,
  ): List<UserAccount>

  // --- Trash queries ---

  @Query(
    value = """
      select k.id as id, ns.name as namespace, k.name as name,
             km.description as description, k.deleted_at as deletedAt,
             null as baseTranslation, null as translation, k.is_plural as plural,
             dbu.id as deletedByUserId, dbu.username as deletedByUserUsername,
             dbu.name as deletedByUserName, dbu.avatar_hash as deletedByUserAvatarHash,
             case when dbu.deleted_at is not null then true else false end as deletedByUserDeleted
      from key k
      left join namespace ns on k.namespace_id = ns.id
      left join branch br on k.branch_id = br.id
      left join key_meta km on k.id = km.key_id
      left join user_account dbu on k.deleted_by_id = dbu.id
      where k.project_id = :projectId
        and k.deleted_at is not null
        and ((br.name = :branch and br.deleted_at is null)
             or (:branch is null and (br.id is null or br.is_default)))
      order by k.deleted_at desc, k.id asc
    """,
    countQuery = """
      select count(k.id) from key k
      left join branch br on k.branch_id = br.id
      where k.project_id = :projectId
        and k.deleted_at is not null
        and ((br.name = :branch and br.deleted_at is null)
             or (:branch is null and (br.id is null or br.is_default)))
    """,
    nativeQuery = true,
  )
  fun findSoftDeletedByProjectId(
    projectId: Long,
    branch: String?,
    pageable: Pageable,
  ): Page<KeySearchResultView>

  @Query("select k.id from Key k where k.deletedAt is not null and k.deletedAt < :before order by k.id")
  fun findSoftDeletedIdsBefore(
    before: Date,
    pageable: Pageable,
  ): Page<Long>

  @Query(
    """
    from Key k
    left join fetch k.namespace
    left join fetch k.branch
    where k.id in :ids and k.project.id = :projectId and k.deletedAt is not null
    """,
  )
  fun findSoftDeletedByIdsAndProjectId(
    ids: Collection<Long>,
    projectId: Long,
  ): List<Key>
}
