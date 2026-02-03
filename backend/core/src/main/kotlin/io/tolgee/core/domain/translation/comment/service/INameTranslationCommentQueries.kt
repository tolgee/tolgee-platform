package io.tolgee.core.domain.translation.comment.service

import io.tolgee.model.translation.TranslationComment
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface INameTranslationCommentQueries : JpaRepository<TranslationComment, Long> {
  /**
   * Fetches comments for a translation, verifying the translation belongs to the project.
   * The project check is included deliberately as a security measure to prevent unauthorized
   * access to comments from translations in other projects.
   */
  @Query(
    """
    select tc from TranslationComment tc
    left join fetch tc.author
    where tc.translation.id = :translationId
      and tc.translation.key.project.id = :projectId
    """,
  )
  fun getPagedByProjectAndTranslationId(
    projectId: Long,
    translationId: Long,
    pageable: Pageable,
  ): Page<TranslationComment>
}
