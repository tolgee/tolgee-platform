package io.tolgee.repository

import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.SimpleTranslationView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface TranslationRepository : JpaRepository<Translation, Long> {
    @Query("""select t.text as text, l.tag as languageTag, k.name as key from Translation t 
        join t.key k join t.language l where t.key.project.id = :projectId and l.tag in :languages""")
    fun getTranslations(languages: Set<String>, projectId: Long): List<SimpleTranslationView>

    @Query("from Translation t join fetch Key k on t.key = k where k = :key and k.project = :project and t.language in :languages")
    fun getTranslations(key: Key, project: io.tolgee.model.Project, languages: Collection<Language>): Set<Translation>
    fun findOneByKeyAndLanguage(key: Key, language: Language): Optional<Translation>

    @Query("""
        from Translation t join fetch t.key k left join fetch k.keyMeta where t.language.id = :languageId
    """)
    fun getAllByLanguageId(languageId: Long): List<Translation>
    fun getAllByKeyIdIn(keyIds: Iterable<Long>): Iterable<Translation>

    @Modifying
    @Transactional
    @Query("""delete from Translation t where t.key in 
        (select k from t.key k where k.project.id = :projectId)""")
    fun deleteAllByProjectId(projectId: Long)
}
