package io.tolgee.repository;

import io.tolgee.model.key.KeyCodeReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyCoreReferenceRepository extends JpaRepository<KeyCodeReference, Long> {
}
