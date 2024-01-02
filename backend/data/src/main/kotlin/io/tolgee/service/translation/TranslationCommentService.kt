package io.tolgee.service.translation

import io.tolgee.dtos.request.translation.comment.ITranslationCommentDto
import io.tolgee.dtos.request.translation.comment.TranslationCommentDto
import io.tolgee.dtos.request.translation.comment.TranslationCommentWithLangKeyDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.repository.TranslationRepository
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
  private val translationService: TranslationService,
  private val translationRepository: TranslationRepository,
) {
  @Transactional
  fun create(
    dto: TranslationCommentWithLangKeyDto,
    projectId: Long,
    author: UserAccount,
  ): TranslationComment {
    val translation = translationService.getOrCreate(dto.keyId, dto.languageId, projectId)

    if (translation.id == 0L) {
      translation.state = TranslationState.UNTRANSLATED
    }

    return create(dto, translation, author)
  }

  @Transactional
  fun create(
    dto: ITranslationCommentDto,
    translationId: Long,
    projectId: Long,
    author: UserAccount,
  ): TranslationComment {
    val translation = translationService.get(translationId, projectId)

    return TranslationComment(
      text = dto.text,
      state = dto.state,
      translation = translation,
    ).let {
      it.author = author
      create(it)
    }
  }

  private fun create(
    dto: ITranslationCommentDto,
    translation: Translation,
    author: UserAccount,
  ): TranslationComment {
    return TranslationComment(
      text = dto.text,
      state = dto.state,
      translation = translation,
    ).let {
      it.author = author
      create(it)
    }
  }

  @Transactional
  fun find(id: Long): TranslationComment? {
    return translationCommentRepository.findById(id).orElse(null)
  }

  @Transactional
  fun get(id: Long): TranslationComment {
    return find(id) ?: throw NotFoundException()
  }

  @Transactional
  fun findWithAuthorFetched(id: Long): TranslationComment? {
    return translationCommentRepository.findWithFetchedAuthor(id)
  }

  @Transactional
  fun getWithAuthorFetched(id: Long): TranslationComment {
    return findWithAuthorFetched(id) ?: throw NotFoundException()
  }

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
  ): Page<TranslationComment> {
    return translationCommentRepository.getPagedByTranslation(translation, pageable)
  }

  @Transactional
  fun delete(entity: TranslationComment) {
    deleteByIds(listOf(entity.id))
  }

  @Transactional
  fun deleteByIds(ids: List<Long>) {
    return translationCommentRepository.deleteAllByIdIn(ids)
  }

  fun create(entity: TranslationComment): TranslationComment {
    return translationCommentRepository.save(entity)
  }

  fun saveAll(entities: Collection<TranslationComment>) {
    translationCommentRepository.saveAll(entities)
  }

  fun update(
    entity: TranslationComment,
    updatedBy: UserAccount = authenticationFacade.authenticatedUserEntity,
  ): TranslationComment {
    return translationCommentRepository.save(entity)
  }

  fun deleteByTranslationIdIn(ids: Collection<Long>) {
    return translationCommentRepository.deleteByTranslationIdIn(ids)
  }

  fun deleteAllByProject(projectId: Long) {
    entityManager.createNativeQuery(
      "DELETE FROM translation_comment WHERE translation_id IN " +
        "(SELECT id FROM translation WHERE key_id IN " +
        "(SELECT id FROM key WHERE project_id = :projectId))",
    ).setParameter("projectId", projectId).executeUpdate()
  }
}
