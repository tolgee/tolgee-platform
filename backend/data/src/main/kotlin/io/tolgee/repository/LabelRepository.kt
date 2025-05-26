package io.tolgee.repository

import io.tolgee.model.translation.Label
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface LabelRepository : JpaRepository<Label, Long> {

  fun findByProjectId(projectId: Long, pageable: Pageable): Page<Label>

  fun findByProjectIdAndId(projectId: Long, labelId: Long): Optional<Label>
}
