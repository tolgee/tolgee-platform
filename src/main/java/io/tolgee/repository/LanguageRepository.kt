package io.tolgee.repository

import io.tolgee.model.Language
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LanguageRepository : JpaRepository<Language, Long> {
    fun findByAbbreviationAndProject(abbreviation: String, project: io.tolgee.model.Project): Optional<Language>
    fun findByNameAndProject(name: String?, project: io.tolgee.model.Project): Optional<Language>
    fun findByAbbreviationAndProjectId(abbreviation: String?, projectId: Long): Optional<Language>
    fun findAllByProjectId(projectId: Long?): Set<Language>
    fun findAllByAbbreviationInAndProjectId(abbreviation: Collection<String?>?, projectId: Long?): Set<Language>
    fun deleteAllByProjectId(projectId: Long?)
}
