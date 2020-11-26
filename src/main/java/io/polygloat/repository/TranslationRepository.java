package io.polygloat.repository;

import io.polygloat.model.Language;
import io.polygloat.model.Key;
import io.polygloat.model.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    Set<Translation> getTranslations(Key key, io.polygloat.model.Repository repository, Collection<Language> languages);

    Optional<Translation> findOneByKeyAndLanguage(Key key, Language language);

    Set<Translation> getAllByLanguageId(Long languageId);

    @Modifying
    @Query("delete from Translation t where t.key.id in (select s.id from Key s where s.repository.id = :repositoryId)")
    void deleteAllByRepositoryId(Long repositoryId);

    void deleteAllByLanguageId(Long languageId);

    @Modifying
    @Query("delete from Translation t where t.key.id in :ids")
    void deleteAllByKeyIds(Collection<Long> ids);

    void deleteAllByKeyId(Long id);
}
