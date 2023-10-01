package io.tolgee.repository.cdn

import io.tolgee.model.cdn.CdnStorage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CdnStorageRepository : JpaRepository<CdnStorage?, Long?> {
  fun findAllByProjectId(projectId: Long, pageable: Pageable): Page<CdnStorage>
  fun getByProjectIdAndId(projectId: Long, cdnId: Long): CdnStorage
}
