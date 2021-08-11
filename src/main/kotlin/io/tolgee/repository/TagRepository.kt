package io.tolgee.repository

import io.tolgee.model.Project
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TagRepository : JpaRepository<Tag, Long> {
  fun findByProjectAndName(project: Project, name: String): Tag?

  @Query(
    """
    from Tag t where t.project.id = :projectId
    and (:search is null or lower(name) like lower(concat('%', cast(:search as text),'%')))
  """
  )
  fun findAllByProject(projectId: Long, search: String? = null, pageable: Pageable): Page<Tag>

  @Query(
    """
    from Key k
    left join fetch k.keyMeta km
    left join fetch km.tags
    where k.id in :keyIds
  """
  )
  fun getKeysWithTags(keyIds: Iterable<Long>): List<Key>

  @Query(
    """
    from ImportKey k
    join fetch k.keyMeta km
    join fetch km.tags
    where k.id in :keyIds
  """
  )
  fun getImportKeysWithTags(keyIds: Iterable<Long>): List<ImportKey>

  @Query(
    """
    from Tag t
    join fetch t.keyMetas
    where t.id in :tagIds
    """
  )
  fun getTagsWithKeyMetas(tagIds: Iterable<Long>): List<Tag>
}
