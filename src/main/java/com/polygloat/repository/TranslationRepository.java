package com.polygloat.repository;

import com.polygloat.model.Language;
import com.polygloat.model.Source;
import com.polygloat.model.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {

    @Query("from Translation t join fetch t.source where t.source.repository.id = :repositoryId and t.language.abbreviation in :languages")
    Set<Translation> getTranslations(Set<String> languages, Long repositoryId);

    @Query("from Translation t join fetch Source s on t.source = s where s = :source and s.repository = :repository and t.language in :languages")
    Set<Translation> getTranslations(Source source, com.polygloat.model.Repository repository, Collection<Language> languages);

    Optional<Translation> findOneBySourceAndLanguage(Source source, Language language);

    @Modifying
    @Query("delete from Translation t where t.source.id in (select s.id from Source s where s.repository.id = :repositoryId)")
    void deleteAllByRepositoryId(Long repositoryId);

    void deleteAllByLanguageId(Long languageId);

    @Modifying
    @Query("delete from Translation t where t.source.id in :ids")
    void deleteAllBySourceIds(Collection<Long> ids);

    void deleteAllBySourceId(Long id);
}
