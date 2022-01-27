package io.tolgee.repository

import io.tolgee.model.AutoTranslationConfig
import io.tolgee.model.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AutoTranslationConfigRepository : JpaRepository<AutoTranslationConfig, Long> {
  fun findOneByProject(project: Project): AutoTranslationConfig?
}
