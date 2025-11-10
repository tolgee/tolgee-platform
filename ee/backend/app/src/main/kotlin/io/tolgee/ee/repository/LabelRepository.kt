package io.tolgee.ee.repository

import io.tolgee.model.translation.Label
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LabelRepository : JpaRepository<Label, Long> {
  @Query(
    """
    from Label l
    where l.project.id = :projectId
    and (:search is null or lower(l.name) like lower(concat('%', cast(:search AS text), '%')))
  """,
  )
  fun findByProjectId(
    projectId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<Label>

  fun findAllByProjectIdAndIdIn(
    projectId: Long,
    ids: List<Long>,
  ): List<Label>

  fun findAllByProjectIdAndName(
    projectId: Long,
    name: String,
  ): List<Label>

  fun findByProjectIdAndId(
    projectId: Long,
    labelId: Long,
  ): Label?

  @Query(
    """
    from Label l
    join l.translations t
    where t.id in :translationIds
    order by l.name
    """,
  )
  fun findByTranslationsIdIn(translationIds: List<Long>): List<Label>

  @Query(
    """
    select l.project.id
    from Label l
    where l.id in :labelIds
  """,
  )
  fun getProjectIdsForLabelIds(labelIds: List<Long>): List<Long>

  fun findAllByProjectId(projectId: Long): List<Label>
}
