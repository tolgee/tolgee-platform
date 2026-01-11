package io.tolgee.repository.contentDelivery

import io.tolgee.model.contentDelivery.ContentStorage
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ContentStorageRepository : JpaRepository<ContentStorage?, Long?> {
  fun findAllByProjectId(
    projectId: Long,
    pageable: Pageable,
  ): Page<ContentStorage>

  fun getByProjectIdAndId(
    projectId: Long,
    contentDeliveryConfigId: Long,
  ): ContentStorage

  @Query(
    """
    select count(*) > 0 from ContentDeliveryConfig cdc where cdc.contentStorage = :storage
  """,
  )
  fun isStorageInUse(storage: ContentStorage): Boolean
}
