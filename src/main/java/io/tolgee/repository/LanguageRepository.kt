package io.tolgee.repository

import io.tolgee.model.Language
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LanguageRepository : JpaRepository<Language, Long> {
    fun findByAbbreviationAndRepository(abbreviation: String, repository: io.tolgee.model.Repository): Optional<Language>
    fun findByNameAndRepository(name: String?, repository: io.tolgee.model.Repository): Optional<Language>
    fun findByAbbreviationAndRepositoryId(abbreviation: String?, repositoryId: Long): Optional<Language>
    fun findAllByRepositoryId(repositoryId: Long?): Set<Language>
    fun findAllByAbbreviationInAndRepositoryId(abbreviation: Collection<String?>?, repositoryId: Long?): Set<Language>
    fun deleteAllByRepositoryId(repositoryId: Long?)
}
