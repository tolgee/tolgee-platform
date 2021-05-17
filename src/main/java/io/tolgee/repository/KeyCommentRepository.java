package io.tolgee.repository;

import io.tolgee.model.key.KeyComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyCommentRepository extends JpaRepository<KeyComment, Long> {
}
