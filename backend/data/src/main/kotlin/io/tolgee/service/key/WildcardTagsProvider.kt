package io.tolgee.service.key

import io.tolgee.model.Project_
import io.tolgee.model.key.Tag
import io.tolgee.model.key.Tag_
import jakarta.persistence.EntityManager

class WildcardTagsProvider(
  private val entityManager: EntityManager,
) {
  fun getTagsWithAppliedWildcards(
    projectId: Long,
    tags: Collection<String>,
  ): Set<String> {
    val nonWildcardTags = mutableSetOf<String>()
    val likeStrings =
      tags.mapNotNull {
        if (!it.contains("*")) {
          nonWildcardTags.add(it)
          return@mapNotNull null
        }

        it.escapeLikeForPostgres().replace("\\*+".toRegex(), "%")
      }

    if (likeStrings.isEmpty()) {
      return nonWildcardTags.toSet()
    }

    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(String::class.java)
    val root = query.from(Tag::class.java)
    query.select(root.get(Tag_.name))

    val tagNameConditions =
      likeStrings.map {
        cb.like(root.get(Tag_.name), it)
      }

    query.where(
      cb.and(
        cb.equal(root.get(Tag_.project).get(Project_.id), projectId),
        cb.or(*tagNameConditions.toTypedArray()),
      ),
    )

    val wildcardTags = entityManager.createQuery(query).resultList
    return (wildcardTags + nonWildcardTags).toSet()
  }

  private fun String.escapeLikeForPostgres(): String {
    return replace("%", "!%")
  }
}
