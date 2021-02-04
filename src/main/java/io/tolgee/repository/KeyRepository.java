package io.tolgee.repository;

import io.tolgee.model.Key;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface KeyRepository extends JpaRepository<Key, Long> {
    @Query("from Key k " +
            "join fetch Translation t on t.key = k and t.language.abbreviation in :languages " +
            "where k.repository.id = :repositoryId")
    Set<Key> getKeysWithTranslations(List<String> languages, Long repositoryId);

    Optional<Key> getByNameAndRepository(String name, io.tolgee.model.Repository repository);

    Optional<Key> getByNameAndRepositoryId(Object fullPathString, Long repositoryId);

    Set<Key> getAllByRepositoryId(long repositoryId);

    @Modifying
    @Query("delete from Key s where s.repository.id = :repositoryId")
    void deleteAllByRepositoryId(Long repositoryId);

    void deleteAllByIdIn(Collection<Long> ids);
}
