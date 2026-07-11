package io.tolgee.ee.component.slackIntegration.notification

import io.tolgee.ee.component.slackIntegration.data.SlackKeyInfoDto
import io.tolgee.ee.component.slackIntegration.data.SlackTranslationInfoDto
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
  private val translationCache = mutableMapOf<Long, List<SlackTranslationInfoDto>>()
  private val keyCache = mutableMapOf<Long, SlackKeyInfoDto>()

  fun getKeyTranslations(keyId: Long): List<SlackTranslationInfoDto> {
    return translationCache.getOrPut(keyId) {
      getTranslationsViaEntityManager(keyId)
    }
  }

  fun getTranslationById(translationId: Long): SlackTranslationInfoDto? {
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

  private fun tryToFindInCache(translationId: Long): SlackTranslationInfoDto? {
    translationCache.forEach { (_, translations) ->
      translations.find { it.translationId == translationId }?.let {
        return it
      }
    }
    return null
  }

  fun getKeyInfo(keyId: Long): SlackKeyInfoDto {
    return keyCache.getOrPut(keyId) {
      getKeyInfoViaEntityManager(keyId)
    }
  }

  private fun getKeyInfoViaEntityManager(keyId: Long): SlackKeyInfoDto {
    val result =
      entityManager
        .createQuery(
          """
      |SELECT k.id, k.name, t.name, n.name, km.description
      |FROM Key k
      |left join k.keyMeta km
      |left join km.tags t
      |left join k.namespace n
      |    WHERE k.id = :keyId
      |
          """.trimMargin(),
          Array::class.java,
        ).setParameter("keyId", keyId)
        .resultList

    return result
      .groupBy { it[0] }
      .map {
        SlackKeyInfoDto(
          id = it.key as Long,
          name = it.value.firstOrNull()?.get(1) as String,
          tags =
            it.value
              .mapNotNull { row -> row[2] as String? }
              .toSet()
              .nullIfEmpty(),
          namespace = it.value.firstOrNull()?.get(3) as String?,
          description = it.value.firstOrNull()?.get(4) as String?,
        )
      }.single()
  }

  fun getTranslation(
    keyId: Long,
    languageTag: String,
  ): SlackTranslationInfoDto? {
    return getKeyTranslations(keyId).find { it.languageTag == languageTag }
  }

  private fun getTranslationsViaEntityManager(keyId: Long): MutableList<SlackTranslationInfoDto> =
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
  private fun getTranslationsViaEntityManagerByTranslationId(
    translationId: Long,
  ): MutableList<SlackTranslationInfoDto> =
    getQuery()
      .setParameter("keyId", null)
      .setParameter("translationId", translationId)
      .resultList

  private fun getQuery(): TypedQuery<SlackTranslationInfoDto> =
    entityManager.createQuery(
      """SELECT new 
            |io.tolgee.ee.component.slackIntegration.data.SlackTranslationInfoDto(
            |    t2.key.id, t2.id, t2.language.tag, t2.language.id, 
            |    t2.language.name, t2.language.flagEmoji, t2.text, t2.state
            |)
            |FROM Translation t
            |    join t.key k
            |    join k.translations t2
            |    WHERE (t.key.id = :keyId or t.id = :translationId) and
            |          (:keyId is not null or :translationId is not null) and
            |          t2.language.deletedAt is null and 
            |          t2.language.project.deletedAt is null
            |
      """.trimMargin(),
      SlackTranslationInfoDto::class.java,
    )

  private val entityManager: EntityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }
}
