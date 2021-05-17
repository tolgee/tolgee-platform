package io.tolgee.repository;

import io.tolgee.model.key.KeyMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyMetaRepository extends JpaRepository<KeyMeta, Long> {
}
