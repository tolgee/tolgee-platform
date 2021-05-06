package io.tolgee.repository;

import io.tolgee.model.Key;
import io.tolgee.model.Language;
import io.tolgee.model.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {

    @Query("from Translation t join fetch t.key where t.key.repository.id = :repositoryId and t.language.abbreviation in :languages")
    Set<Translation> getTranslations(Set<String> languages, Long repositoryId);

    @Query("from Translation t join fetch Key k on t.key = k where k = :key and k.repository = :repository and t.language in :languages")
    Set<Translation> getTranslations(Key key, io.tolgee.model.Repository repository, Collection<Language> languages);

    Optional<Translation> findOneByKeyAndLanguage(Key key, Language language);

    Set<Translation> getAllByLanguageId(Long languageId);

    Iterable<Translation> getAllByKeyRepositoryId(Long repositoryId);

    Iterable<Translation> getAllByKeyIdIn(Iterable<Long> keyIds);
}
