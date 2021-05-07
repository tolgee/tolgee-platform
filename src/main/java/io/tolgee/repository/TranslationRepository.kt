package io.tolgee.repository

import io.tolgee.model.Key
import io.tolgee.model.Language
import io.tolgee.model.Translation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TranslationRepository : JpaRepository<Translation, Long> {
    @Query("from Translation t join fetch t.key where t.key.repository.id = :repositoryId and t.language.abbreviation in :languages")
    fun getTranslations(languages: Set<String>, repositoryId: Long): Set<Translation>

    @Query("from Translation t join fetch Key k on t.key = k where k = :key and k.repository = :repository and t.language in :languages")
    fun getTranslations(key: Key, repository: io.tolgee.model.Repository, languages: Collection<Language>): Set<Translation>
    fun findOneByKeyAndLanguage(key: Key, language: Language): Optional<Translation>
    fun getAllByLanguageId(languageId: Long): Set<Translation>
    fun getAllByKeyRepositoryId(repositoryId: Long): Iterable<Translation>
    fun getAllByKeyIdIn(keyIds: Iterable<Long>): Iterable<Translation>
    fun getAllByLanguageRepositoryId(repositoryId: Long): Iterable<Translation>
}
