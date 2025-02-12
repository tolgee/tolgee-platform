package io.tolgee.ee.component.slackIntegration

import io.tolgee.ee.component.slackIntegration.data.KeyInfoDto
import io.tolgee.ee.component.slackIntegration.data.TranslationInfoDto
import io.tolgee.util.nullIfEmpty
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import org.springframework.context.ApplicationContext

/**
 * In the SlackExecutorHelper there is a lot of data fetched from the DB
 * This class should effectively cache the minimal DTOs to use in that class so, we should
 * run as few queries as possible
 */
class SlackIntegrationDataProvider(
  val applicationContext: ApplicationContext,
) {
  private val translationCache = mutableMapOf<Long, List<TranslationInfoDto>>()
  private val keyCache = mutableMapOf<Long, KeyInfoDto>()

  fun getKeyTranslations(keyId: Long): List<TranslationInfoDto> {
    return translationCache.getOrPut(keyId) {
      getTranslationsViaEntityManager(keyId)
    }
  }

  fun getTranslationById(translationId: Long): TranslationInfoDto? {
    val fromCache = tryToFindInCache(translationId)
    if (fromCache != null) {
      return fromCache
    }

    val translations = getTranslationsViaEntityManagerByTranslationId(translationId)

    translations.groupBy { it.keyId }.forEach { (keyId, translations) ->
      translationCache[keyId] = translations
    }

    return tryToFindInCache(translationId)
  }

  private fun tryToFindInCache(translationId: Long): TranslationInfoDto? {
    translationCache.forEach { (_, translations) ->
      translations.find { it.translationId == translationId }?.let {
        return it
      }
    }
    return null
  }

  fun getKeyInfo(keyId: Long): KeyInfoDto {
    return keyCache.getOrPut(keyId) {
      getKeyInfoViaEntityManager(keyId)
    }
  }

  private fun getKeyInfoViaEntityManager(keyId: Long): KeyInfoDto {
    val result =
      entityManager.createQuery(
        """
      |SELECT k.id, t.name, k.name, k.namespace.name, km.description
      |FROM Key k
      |left join k.keyMeta km
      |left join km.tags t
      |    WHERE k.id = :keyId
      |
        """.trimMargin(),
        Array::class.java,
      )
        .setParameter("keyId", keyId)
        .resultList

    return result
      .groupBy { it[0] }
      .map {
        KeyInfoDto(
          id = it.key as Long,
          name = it.value.firstOrNull()?.get(1) as String,
          tags = it.value.mapNotNull { row -> row[2] as String? }.toSet().nullIfEmpty(),
          namespace = it.value.firstOrNull()?.get(3) as String?,
          description = it.value.firstOrNull()?.get(4) as String?,
        )
      }
      .single()
  }

  fun getTranslation(
    keyId: Long,
    languageTag: String,
  ): TranslationInfoDto? {
    return getKeyTranslations(keyId).find { it.languageTag == languageTag }
  }

  private fun getTranslationsViaEntityManager(keyId: Long): MutableList<TranslationInfoDto> =
    getQuery()
      .setParameter("keyId", keyId)
      .setParameter("translationId", null)
      .resultList

  /**
   * Returns all key translations by translation id
   *
   * In other words this returns all "siblings" of the translation with the given id, so we can populate the cache
   * for whole key with all it's translations
   */
  private fun getTranslationsViaEntityManagerByTranslationId(translationId: Long): MutableList<TranslationInfoDto> =
    getQuery()
      .setParameter("keyId", null)
      .setParameter("translationId", translationId)
      .resultList

  private fun getQuery(): TypedQuery<TranslationInfoDto> =
    entityManager.createQuery(
      """SELECT new 
            |io.tolgee.ee.component.slackIntegration.data.TranslationInfoDto(
            |    t.key.id, t.id, t.language.tag, t.language.id, t.language.name, t.language.flagEmoji, t.text, t.state
            |)
            |FROM Translation t 
            |    WHERE (t.key.id = :keyId or t.id = :translationId) and
            |          (:keyId is not null or :translationId is null) and
            |          t.language.deletedAt is null and 
            |          t.language.project.deletedAt is null
            |
      """.trimMargin(),
      TranslationInfoDto::class.java,
    )

  private val entityManager: EntityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }
}
