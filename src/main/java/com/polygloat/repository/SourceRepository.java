package com.polygloat.repository;

import com.polygloat.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SourceRepository extends JpaRepository<Source, Long> {
    @Query("from Source s " +
            "join fetch Translation t on t.source = s and t.language.abbreviation in :languages " +
            "where s.repository.id = :repositoryId")
    Set<Source> getSourcesWithTranslations(List<String> languages, Long repositoryId);

    Optional<Source> getByNameAndRepository(String name, com.polygloat.model.Repository repository);

    Optional<Source> getByNameAndRepositoryId(Object fullPathString, Long repositoryId);

    @Modifying
    @Query("delete from Source s where s.repository.id = :repositoryId")
    void deleteAllByRepositoryId(Long repositoryId);

    void deleteAllByIdIn(Collection<Long> ids);
}
