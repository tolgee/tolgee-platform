package io.tolgee.repository

import io.tolgee.model.translation.Label
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface LabelRepository  : JpaRepository<Label, Long> {

  fun findByProjectId(projectId: Long, pageable: Pageable): Page<Label>

}
