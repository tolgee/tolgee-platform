package io.tolgee.repository;

import io.tolgee.model.key.Key;
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
            "where k.project.id = :projectId")
    Set<Key> getKeysWithTranslations(List<String> languages, Long projectId);

    Optional<Key> getByNameAndProject(String name, io.tolgee.model.Project project);

    Optional<Key> getByNameAndProjectId(Object fullPathString, Long projectId);

    Set<Key> getAllByProjectId(long projectId);

    @Modifying
    @Query("delete from Key s where s.project.id = :projectId")
    void deleteAllByRepositoryId(Long projectId);

    void deleteAllByIdIn(Collection<Long> ids);
}
