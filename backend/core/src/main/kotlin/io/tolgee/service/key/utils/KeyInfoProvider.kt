package io.tolgee.service.key.utils

import io.tolgee.dtos.request.GetKeysRequestDto
import io.tolgee.model.Project_
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Namespace_
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.equalNullable
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import org.springframework.context.ApplicationContext

class KeyInfoProvider(
  applicationContext: ApplicationContext,
  val projectId: Long,
  val dto: GetKeysRequestDto,
) {
  private val entityManager: EntityManager = applicationContext.getBean(EntityManager::class.java)
  private val screenshotService: ScreenshotService = applicationContext.getBean(ScreenshotService::class.java)
  private val translationService: TranslationService = applicationContext.getBean(TranslationService::class.java)

  fun get(): List<Pair<Key, List<Screenshot>>> {
    val cb: CriteriaBuilder = entityManager.criteriaBuilder
    val query = cb.createQuery(Key::class.java)
    val root = query.from(Key::class.java)
    val project = root.join(Key_.project)
    project.on(cb.equal(project.get(Project_.id), projectId))
    val namespace = root.fetch(Key_.namespace, JoinType.LEFT) as Join<Key, Namespace>
    val keyMeta = root.fetch(Key_.keyMeta, JoinType.LEFT)
    keyMeta.fetch(KeyMeta_.tags, JoinType.LEFT)
    val predicates =
      dto.keys.map { key ->
        cb.and(
          cb.equal(root.get(Key_.name), key.name),
          cb.equalNullable(namespace.get(Namespace_.name), key.namespace),
        )
      }

    val keyPredicates = cb.or(*predicates.toTypedArray())

    query.where(keyPredicates)
    query.orderBy(cb.asc(namespace.get(Namespace_.name)), cb.asc(root.get(Key_.name)))

    val result = entityManager.createQuery(query).resultList
    val screenshots = screenshotService.getScreenshotsForKeys(result.map { it.id })

    val translations =
      translationService
        .getForKeys(result.map { it.id }, dto.languageTags)
        .groupBy { it.key.id }

    result.map {
      it.translations = translations[it.id]?.toMutableList() ?: mutableListOf()
    }

    return result.map { it to (screenshots[it.id] ?: listOf()) }.toList()
  }
}
