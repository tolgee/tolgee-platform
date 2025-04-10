package io.tolgee.service.translation

import io.tolgee.dtos.request.translation.comment.ITranslationCommentDto
import io.tolgee.dtos.request.translation.comment.TranslationCommentDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.repository.translation.TranslationCommentRepository
import io.tolgee.security.authentication.AuthenticationFacade
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TranslationCommentService(
  private val translationCommentRepository: TranslationCommentRepository,
  private val authenticationFacade: AuthenticationFacade,
  private val entityManager: EntityManager,
) {
  @Transactional
  fun create(
    dto: ITranslationCommentDto,
    translation: Translation,
    author: UserAccount,
  ): TranslationComment =
    TranslationComment(
      text = dto.text,
      state = dto.state,
      translation = translation,
    ).let {
      it.author = author
      create(it)
    }

  @Transactional
  fun find(id: Long): TranslationComment? = translationCommentRepository.findById(id).orElse(null)

  @Transactional
  fun find(
    projectId: Long,
    translationId: Long,
    commentId: Long,
  ): TranslationComment? = translationCommentRepository.find(projectId, translationId, commentId)

  @Transactional
  fun get(
    projectId: Long,
    translationId: Long,
    commentId: Long,
  ): TranslationComment = find(projectId, translationId, commentId) ?: throw NotFoundException()

  @Transactional
  fun findWithAuthorFetched(
    projectId: Long,
    translationId: Long,
    commentId: Long,
  ): TranslationComment? = translationCommentRepository.findWithFetchedAuthor(projectId, translationId, commentId)

  @Transactional
  fun getWithAuthorFetched(
    projectId: Long,
    translationId: Long,
    commentId: Long,
  ): TranslationComment = findWithAuthorFetched(projectId, translationId, commentId) ?: throw NotFoundException()

  @Transactional
  fun update(
    dto: TranslationCommentDto,
    entity: TranslationComment,
  ): TranslationComment {
    entity.text = dto.text
    entity.state = dto.state
    return this.update(entity)
  }

  @Transactional
  fun setState(
    entity: TranslationComment,
    state: TranslationCommentState,
  ): TranslationComment {
    entity.state = state
    return this.update(entity)
  }

  fun getPaged(
    translation: Translation,
    pageable: Pageable,
  ): Page<TranslationComment> = translationCommentRepository.getPagedByTranslation(translation, pageable)

  @Transactional
  fun delete(entity: TranslationComment) {
    deleteByIds(listOf(entity.id))
  }

  @Transactional
  fun deleteByIds(ids: List<Long>) = translationCommentRepository.deleteAllByIdIn(ids)

  fun create(entity: TranslationComment): TranslationComment = translationCommentRepository.save(entity)

  fun saveAll(entities: Collection<TranslationComment>) {
    translationCommentRepository.saveAll(entities)
  }

  fun update(
    entity: TranslationComment,
    updatedBy: UserAccount = authenticationFacade.authenticatedUserEntity,
  ): TranslationComment = translationCommentRepository.save(entity)

  fun deleteByTranslationIdIn(ids: Collection<Long>) = translationCommentRepository.deleteByTranslationIdIn(ids)

  fun deleteAllByProject(projectId: Long) {
    entityManager
      .createNativeQuery(
        "DELETE FROM translation_comment WHERE translation_id IN " +
          "(SELECT id FROM translation WHERE key_id IN " +
          "(SELECT id FROM key WHERE project_id = :projectId))",
      ).setParameter("projectId", projectId)
      .executeUpdate()
  }
}
