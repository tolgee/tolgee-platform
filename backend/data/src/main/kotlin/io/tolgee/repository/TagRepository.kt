package io.tolgee.repository

import io.tolgee.model.Project
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface TagRepository : JpaRepository<Tag, Long> {
  fun findByProjectAndName(
    project: Project,
    name: String,
  ): Tag?

  @Query(
    """
    from Tag t where t.project.id = :projectId
    and (:search is null or lower(name) like lower(concat('%', cast(:search as text),'%')))
  """,
  )
  fun findAllByProject(
    projectId: Long,
    search: String? = null,
    pageable: Pageable,
  ): Page<Tag>

  @Query(
    """
    from Tag t where t.project.id = :projectId and t.name in :tags
  """,
  )
  fun findAllByProject(
    projectId: Long,
    tags: Collection<String>,
  ): List<Tag>

  @Query(
    """
    from Key k
    left join fetch k.keyMeta km
    left join fetch km.tags
    where k.id in :keyIds
  """,
  )
  fun getKeysWithTags(keyIds: Iterable<Long>): List<Key>

  @Query(
    """
    from ImportKey k
    join fetch k.keyMeta km
    join fetch km.tags
    where k.id in :keyIds
  """,
  )
  fun getImportKeysWithTags(keyIds: Iterable<Long>): List<ImportKey>

  @Query(
    """
    from Tag t
    join fetch t.keyMetas
    where t.id in :tagIds
    """,
  )
  fun getTagsWithKeyMetas(tagIds: Iterable<Long>): List<Tag>

  fun findAllByProjectId(projectId: Long): List<Tag>

  @Modifying(flushAutomatically = true)
  @Query(
    """
    with keys as (
        select t.id from tag t
        left join key_meta_tags kmt on t.id = kmt.tags_id
        left join key_meta km on kmt.key_metas_id = km.id
        where t.project_id = :projectId
        group by t.id having count(km) = 0
    )
      delete from tag t
      where t.id in (select * from keys)
  """,
    nativeQuery = true,
    countQuery = """
      select count(t.id) from tag t
      left join key_meta_tags kmt on t.id = kmt.tags_id
      left join key_meta km on kmt.key_metas_id = km.id
      where t.project_id = :projectId
      group by t.id having count(km) = 0
    """,
  )
  fun deleteAllUnused(projectId: Long)

  @Modifying(flushAutomatically = true)
  @Query(
    """
    delete from Tag t 
    where t.id in (select tag.id from Tag tag join tag.keyMetas km join km.key k where k in :keys)
  """,
  )
  fun deleteAllByKeyIn(keys: Collection<Key>)

  @Query(
    """
    from Tag t
    join fetch t.keyMetas km
    where km.key.id in :keyIds
  """,
  )
  fun getAllByKeyIds(keyIds: Collection<Long>): List<Tag>

  @Query(
    """
    from Tag t left join fetch t.keyMetas km where t.project.id = :projectId and t.id = :tagId
  """,
  )
  fun findWithKeyMetasFetched(
    projectId: Long,
    tagId: Long,
  ): Tag?
}
