package io.tolgee.service.translation

import io.tolgee.dtos.KeyAndLanguage
import io.tolgee.model.Language_
import io.tolgee.model.Project_
import io.tolgee.model.key.Key_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import org.springframework.stereotype.Component

@Component
class TranslationUpdateHelper(
  private val entityManager: EntityManager,
) {
  private fun getQueryToFindExistingTranslations(
    items: Collection<KeyAndLanguage>,
    projectId: Long,
  ): CriteriaQuery<Translation> {
    val cb: CriteriaBuilder = entityManager.criteriaBuilder
    val query = cb.createQuery(Translation::class.java)
    val root = query.from(Translation::class.java)
    val key = root.join(Translation_.key)
    val predicates =
      items.map { item ->
        cb.and(
          cb.equal(key.get(Key_.id), item.key),
          cb.equal(root.get(Translation_.language).get(Language_.id), item.language),
        )
      }
    val keyPredicates = cb.or(*predicates.toTypedArray())
    query.where(cb.and(keyPredicates, cb.equal(key.get(Key_.project).get(Project_.id), projectId)))
    query.select(root)
    return query
  }

  fun getExistingTranslations(
    items: Collection<KeyAndLanguage>,
    projectId: Long,
  ): Map<KeyAndLanguage, Translation> {
    return entityManager
      .createQuery(getQueryToFindExistingTranslations(items, projectId)).resultList.associateBy {
        KeyAndLanguage(it.key, it.language)
      }
  }
}
