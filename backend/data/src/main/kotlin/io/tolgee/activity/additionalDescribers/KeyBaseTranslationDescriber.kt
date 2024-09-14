package io.tolgee.activity.additionalDescribers

import io.tolgee.activity.ActivityAdditionalDescriber
import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.activity.data.RevisionType
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityEntityWithDescription
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class KeyBaseTranslationDescriber(
  private val entityManager: EntityManager,
) : ActivityAdditionalDescriber {
  companion object {
    const val FIELD_NAME: String = "baseTranslation"
  }

  override fun describe(
    activityRevision: ActivityRevision,
    modifiedEntities: ModifiedEntitiesType,
  ) {
    val relevantTranslationModifiedEntities = getRelevantModifiedEntities(activityRevision, modifiedEntities)
    val relevantTranslationDescribingEntities = getRelevantDescribingEntities(activityRevision)
    val toDescribe = getToDescribe(activityRevision, modifiedEntities)
    val newKeyIds =
      modifiedEntities[Key::class]?.mapNotNull {
        if (it.value.revisionType == RevisionType.ADD) it.key.id else null
      }?.toSet() ?: emptySet()

    toDescribe.removeIf { (id, entity) ->
      // if entity is modified in current revision, we take it from there
      relevantTranslationModifiedEntities[id]?.let {
        entity.describe(getFromModifiedEntity(it))
        return@removeIf true
      }

      // if entity is described in current revision, we take it from there
      relevantTranslationDescribingEntities[id]?.let {
        entity.describe(getFromDescribingEntity(it))
        return@removeIf true
      }

      // if base value is not set for new key it doesn't make sense to search for it in the database
      if (newKeyIds.contains(id)) {
        entity.describe(null)
        return@removeIf true
      }

      false
    }

    // other entities are taken from the database
    describeRest(activityRevision, toDescribe)
  }

  private fun ActivityEntityWithDescription.describe(baseText: String?) {
    val additionalDescription = initAdditionalDescription()
    additionalDescription[FIELD_NAME] = BaseTranslationDescription(baseText)
  }

  private fun getToDescribe(
    activityRevision: ActivityRevision,
    allModifiedEntities: ModifiedEntitiesType,
  ): MutableList<Pair<Long, ActivityEntityWithDescription>> {
    val describingRelations =
      activityRevision.describingRelations.mapNotNull {
        if (it.entityClass != "Key") return@mapNotNull null
        it.entityId to it
      }

    val modifiedEntities = allModifiedEntities[Key::class]?.map { it.key.id to it.value } ?: emptyList()

    return (describingRelations + modifiedEntities).toMutableList()
  }

  private fun describeRest(
    activityRevision: ActivityRevision,
    toDescribe: MutableList<Pair<Long, ActivityEntityWithDescription>>,
  ) {
    if (toDescribe.isEmpty()) {
      return
    }

    val keyIds = toDescribe.map { it.first }
    val result =
      entityManager.createQuery(
        "select t.key.id, t.text from Translation t where t.key.id in :keyIds and t.language.id = :languageId",
        Array<Any>::class.java,
      )
        .setParameter("keyIds", keyIds.toSet())
        .setParameter("languageId", activityRevision.baseLanguageId)
        .resultList

    return result.forEach {
      val keyId = it[0] as Long
      val text = it[1] as? String
      toDescribe
        .filter { (toDescribeId) -> toDescribeId == keyId }
        .forEach { (_, entity) ->
          entity.describe(text)
        }
    }
  }

  private fun getFromDescribingEntity(it: ActivityDescribingEntity): String? {
    return it.data["text"] as? String
  }

  private fun getFromModifiedEntity(entity: ActivityModifiedEntity): String? {
    val text = entity.modifications["text"]?.new ?: entity.describingData?.get("text")
    return text as? String
  }

  private fun getRelevantModifiedEntities(
    activityRevision: ActivityRevision,
    modifiedEntities: ModifiedEntitiesType,
  ): Map<Long, ActivityModifiedEntity> {
    val baseLanguageId = activityRevision.baseLanguageId

    return modifiedEntities[Translation::class]?.values?.mapNotNull {
      val languageId = it.languageId ?: return@mapNotNull null
      if (languageId != baseLanguageId) return@mapNotNull null
      val keyId = it.keyId ?: return@mapNotNull null
      keyId to it
    }?.toMap() ?: emptyMap()
  }

  private fun getRelevantDescribingEntities(activityRevision: ActivityRevision): Map<Long, ActivityDescribingEntity> {
    val baseLanguageId = activityRevision.baseLanguageId

    return activityRevision.describingRelations.mapNotNull {
      if (it.entityClass != "Translation") return@mapNotNull null
      val languageId = it.languageId ?: return@mapNotNull null
      if (languageId != baseLanguageId) return@mapNotNull null
      val keyId = it.keyId ?: return@mapNotNull null
      keyId to it
    }.toMap()
  }

  val ActivityEntityWithDescription.languageId: Long?
    get() =
      this.describingRelations?.get(Translation::language.name)?.entityId

  val ActivityEntityWithDescription.keyId: Long?
    get() =
      this.describingRelations?.get(Translation::key.name)?.entityId

  data class BaseTranslationDescription(
    val text: String?,
  )
}
