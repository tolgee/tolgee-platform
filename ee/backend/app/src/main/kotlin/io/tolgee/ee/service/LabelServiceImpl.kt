package io.tolgee.ee.service

import io.tolgee.activity.ActivityHolder
import io.tolgee.constants.Message
import io.tolgee.ee.data.label.LabelRequest
import io.tolgee.ee.repository.LabelRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.label.LabelService
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.Optional

@Primary
@Service
class LabelServiceImpl(
  private val labelRepository: LabelRepository,
  private val entityManager: EntityManager,
  private val translationRepository: TranslationRepository,
  @Lazy private val translationService: TranslationService,
  private val activityHolder: ActivityHolder,
) : LabelService {
  override fun getProjectLabels(
    projectId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<Label> {
    return labelRepository.findByProjectId(projectId, pageable, search)
  }

  fun getProjectLabelsByIds(
    projectId: Long,
    ids: List<Long>,
  ): List<Label> {
    return labelRepository.findAllByProjectIdAndIdIn(projectId, ids)
  }

  override fun find(labelId: Long): Optional<Label> {
    return labelRepository.findById(labelId)
  }

  fun findByTranslationIds(translationIds: List<Long>): List<Label> {
    return labelRepository.findByTranslationsIdIn(translationIds)
  }

  override fun getByTranslationIdsIndexed(translationIds: List<Long>): Map<Long, List<Label>> {
    return findByTranslationIds(translationIds)
      .asSequence()
      .flatMap { label ->
        label.translations.asSequence().map { it.id to label }
      }.groupBy({ it.first }, { it.second })
  }

  private fun getByProjectIdAndId(
    projectId: Long,
    labelId: Long,
  ): Label {
    return labelRepository.findByProjectIdAndId(
      projectId,
      labelId,
    ) ?: throw NotFoundException(Message.LABEL_NOT_FOUND)
  }

  @Transactional
  fun createLabel(
    projectId: Long,
    request: LabelRequest,
  ): Label {
    if (labelRepository.findAllByProjectIdAndName(projectId, request.name).isNotEmpty()) {
      throw BadRequestException(Message.LABEL_ALREADY_EXISTS, listOf(request.name))
    }
    val label = Label()
    updateFromRequest(label, request)
    activityHolder.businessEventData["name"] = label.name
    label.project = entityManager.getReference(Project::class.java, projectId)

    labelRepository.save(label)
    return label
  }

  @Transactional
  fun updateLabel(
    projectId: Long,
    labelId: Long,
    request: LabelRequest,
  ): Label {
    val label = getByProjectIdAndId(projectId, labelId)
    if (label.name != request.name) {
      val existingLabels = labelRepository.findAllByProjectIdAndName(projectId, request.name)
      if (existingLabels.any { it.id != labelId }) {
        throw BadRequestException(Message.LABEL_ALREADY_EXISTS, listOf(request.name))
      }
    }
    updateFromRequest(label, request)
    activityHolder.businessEventData["name"] = label.name

    labelRepository.save(label)
    return label
  }

  private fun updateFromRequest(
    label: Label,
    request: LabelRequest,
  ) {
    label.name = request.name
    label.description = request.description
    label.color = request.color.uppercase()
  }

  @Transactional
  fun deleteLabel(
    projectId: Long,
    labelId: Long,
  ) {
    val label = getByProjectIdAndId(projectId, labelId)
    label.clearTranslations()
    labelRepository.delete(label)
  }

  @Transactional
  fun assignLabel(
    projectId: Long,
    translationId: Long,
    labelId: Long,
  ): Label {
    val label = getByProjectIdAndId(projectId, labelId)
    val translation =
      translationRepository.find(
        projectId,
        translationId,
      ) ?: throw NotFoundException(Message.TRANSLATION_NOT_FOUND)
    translation.addLabel(label)
    translationRepository.save(translation)
    labelRepository.save(label)
    return label
  }

  @Transactional
  fun assignLabel(
    projectId: Long,
    translation: Translation,
    labelId: Long,
  ): Label {
    val label = getByProjectIdAndId(projectId, labelId)
    translation.addLabel(label)
    translationRepository.save(translation)
    labelRepository.save(label)
    return label
  }

  @Transactional
  fun unassignLabel(
    projectId: Long,
    translationId: Long,
    labelId: Long,
  ) {
    val label = getByProjectIdAndId(projectId, labelId)
    val translation =
      translationRepository.find(
        projectId,
        translationId,
      ) ?: throw NotFoundException(Message.TRANSLATION_NOT_FOUND)
    translation.removeLabel(label)
    labelRepository.save(label)
  }

  @Transactional
  override fun batchAssignLabels(
    keyIds: List<Long>,
    languageIds: List<Long>,
    labelIds: List<Long>,
  ) {
    val labels = labelRepository.findAllById(labelIds)
    if (labels.isEmpty()) {
      throw NotFoundException(Message.LABEL_NOT_FOUND)
    }

    val existingTranslations = translationRepository.findAllByKeyIdInAndLanguageIdIn(keyIds, languageIds)

    val existingTranslationsMap =
      existingTranslations.groupBy {
        Pair(it.key.id, it.language.id)
      }

    val allTranslations = mutableListOf<Translation>()
    allTranslations.addAll(existingTranslations)

    for (keyId in keyIds) {
      for (languageId in languageIds) {
        val key = Pair(keyId, languageId)
        if (!existingTranslationsMap.containsKey(key)) {
          val translation = translationService.createEmpty(keyId, languageId)
          allTranslations.add(translation)
        }
      }
    }

    allTranslations.forEach { translation ->
      labels.forEach { label ->
        if (!translation.labels.contains(label)) {
          translation.addLabel(label)
        }
      }
    }

    translationRepository.saveAll(allTranslations)
  }

  @Transactional
  override fun batchUnassignLabels(
    keyIds: List<Long>,
    languageIds: List<Long>,
    labelIds: List<Long>,
  ) {
    val labels = labelRepository.findAllById(labelIds)
    if (labels.isEmpty()) {
      throw NotFoundException(Message.LABEL_NOT_FOUND)
    }

    val translations = translationRepository.findAllByKeyIdInAndLanguageIdIn(keyIds, languageIds)
    if (translations.isEmpty()) {
      throw NotFoundException(Message.TRANSLATION_NOT_FOUND)
    }

    translations.forEach { translation ->
      labels.forEach { label ->
        if (translation.labels.contains(label)) {
          translation.removeLabel(label)
        }
      }
    }

    translationRepository.saveAll(translations)
  }

  override fun deleteLabelsByProjectId(projectId: Long) {
    val labels = labelRepository.findAllByProjectId(projectId)
    labels.forEach { label ->
      label.clearTranslations()
    }
    labelRepository.deleteAll(labels)
  }

  override fun getProjectIdsForLabelIds(labelIds: List<Long>): List<Long> {
    return labelRepository.getProjectIdsForLabelIds(labelIds)
  }
}
