package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.MtServiceManager
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.formats.PluralForms
import io.tolgee.service.PromptService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.PluralTranslationUtil.Companion.REPLACE_NUMBER_PLACEHOLDER
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext

class MtTranslatorContext(
  private val projectId: Long,
  val applicationContext: ApplicationContext,
  val isBatch: Boolean,
) {
  val languages by lazy {
    languageService.getProjectLanguages(projectId).associateBy { it.id }
  }

  val project by lazy {
    projectService.getDto(projectId)
  }

  val metadata: MutableMap<MetadataKey, MetadataKey> = mutableMapOf()

  val keys: MutableMap<Long, KeyForMt> = mutableMapOf()

  private val possibleTargetLanguages = mutableSetOf<Long>()

  /**
   * LanguageId -> Set of services
   */
  val enabledServices = mutableMapOf<Long, Set<MtServiceInfo>>()

  /**
   * LanguageId -> Primary Service
   */
  private val primaryServices = mutableMapOf<Long, MtServiceInfo?>()

  private val pluralFormsCache: MutableMap<String, PluralForms?> = mutableMapOf()

  /**
   * Whan translating plurals, we need to replace the "REPLACE_NUMBER" (#) ICU tokens with some placeholder
   * and we want to cache it
   */
  private val pluralFormsWithReplacedReplaceNumberCache: MutableMap<String, PluralForms?> = mutableMapOf()

  fun getServicesToUse(
    targetLanguageId: Long,
    params: MachineTranslationParams,
  ): Set<MtServiceInfo> {
    if (params.useAllEnabledServices) {
      return getEnabledServices(targetLanguageId)
    }
    if (params.usePrimaryService) {
      return getPrimaryService(targetLanguageId)?.let { setOf(it) } ?: emptySet()
    }

    if (params.desiredServices.isNullOrEmpty()) {
      throw IllegalStateException("Desired services not set")
    }

    return getServicesToUseByDesiredServices(targetLanguageId, params.desiredServices)
  }

  fun getServicesToUseByDesiredServices(
    targetLanguageId: Long,
    desiredServices: Set<MtServiceType>?,
  ): Set<MtServiceInfo> {
    val enabledServices = getEnabledServices(targetLanguageId)

    checkServices(
      desired = desiredServices?.toSet(),
      enabled = enabledServices.map { it.serviceType },
    )
    return enabledServices.filter { desiredServices?.contains(it.serviceType) ?: true }
      .toSet()
  }

  fun preparePossibleTargetLanguages(paramsList: List<MachineTranslationParams>) {
    val all = paramsList.flatMap { it.targetLanguageIds + listOfNotNull(it.targetLanguageId) }
    possibleTargetLanguages.addAll(all)
  }

  fun getPluralForms(string: String): PluralForms? {
    return pluralFormsCache.computeIfAbsentSupportNull(string) {
      io.tolgee.formats.getPluralForms(string)
    }
  }

  fun getPluralFormsReplacingReplaceParam(string: String): PluralForms? {
    return pluralFormsWithReplacedReplaceNumberCache.computeIfAbsentSupportNull(string) {
      io.tolgee.formats.getPluralFormsReplacingReplaceParam(string, REPLACE_NUMBER_PLACEHOLDER)
    }
  }

  /**
   * If some primary service is not found, we fetch all missing at once,
   * so it's fetched in one query, but only when required
   */
  private fun getPrimaryService(targetLanguageId: Long): MtServiceInfo? {
    return primaryServices[targetLanguageId] ?: let {
      val missing = possibleTargetLanguages - primaryServices.keys
      mtServiceConfigService.getPrimaryServices(missing.toList(), projectId).forEach {
        primaryServices[it.key] = it.value
      }
      primaryServices[targetLanguageId]
    }
  }

  fun prepareKeys(params: List<MtBatchItemParams>) {
    val keyIds = params.mapNotNull { it.keyId }.filter { !keys.containsKey(it) }
    prepareKeysByIds(keyIds)
  }

  fun prepareKeysByIds(keyIds: List<Long>) {
    val result =
      entityManger.createQuery(
        """
        select new io.tolgee.service.machineTranslation.KeyForMt(k.id, k.name, ns.name, km.description, t.text, k.isPlural )
        from Key k
        left join k.project.baseLanguage bl
        left join k.translations t on t.language.id = bl.id
        left join k.keyMeta km
        left join k.namespace ns
        where k.id in :keyIds and k.project.id = :projectId
      """,
        KeyForMt::class.java,
      )
        .setParameter("keyIds", keyIds)
        .setParameter("projectId", projectId)
        .resultList

    if (result.size != keyIds.size) {
      throw BadRequestException(Message.KEY_NOT_FOUND)
    }

    result.forEach {
      keys[it.id] = it
    }
  }

  private fun checkServices(
    desired: Set<MtServiceType>?,
    enabled: List<MtServiceType>,
  ) {
    if (desired != null && desired.any { !enabled.contains(it) }) {
      throw BadRequestException(Message.MT_SERVICE_NOT_ENABLED)
    }
  }

  val baseLanguage: LanguageDto by lazy {
    languages.values.singleOrNull { it.base } ?: throw IllegalStateException("Base language not found")
  }

  fun getServiceInfo(
    languageId: Long,
    service: MtServiceType,
  ): MtServiceInfo {
    return enabledServices[languageId]?.singleOrNull { it.serviceType == service }
      ?: primaryServices[languageId]
      ?: throw IllegalStateException("Service $service not enabled for language $languageId")
  }

  fun prepareMetadata(batch: List<MtBatchItemParams>) {
    val newMetadataKeys =
      batch.filter { needsMetadata(it) }.mapNotNull {
        val baseTranslationText = it.baseTranslationText ?: return@mapNotNull null
        MetadataKey(
          it.keyId,
          baseTranslationText,
          it.targetLanguageId,
        )
      }.filter { !metadata.containsKey(it) }
    newMetadataKeys.forEach {
      metadata[it] = it
    }
  }

  fun getLanguage(languageId: Long): LanguageDto {
    return languages[languageId] ?: throw IllegalStateException("Language $languageId not found")
  }

  fun getMetadata(item: MtBatchItemParams): MetadataKey? {
    val baseTranslationText =
      item.baseTranslationText
        ?: throw IllegalStateException("Base translation text not found")
    return metadata[MetadataKey(item.keyId, baseTranslationText, item.targetLanguageId)]
  }

  private fun needsMetadata(item: MtBatchItemParams): Boolean {
    val service = getServiceInfo(item.targetLanguageId, item.service)
    return service.serviceType.usesMetadata
  }

  private fun getEnabledServices(languageId: Long): Set<MtServiceInfo> {
    return enabledServices.computeIfAbsent(languageId) {
      val language =
        languages[it]
          ?: throw IllegalStateException("Language $it not found")
      val result = mtServiceConfigService.getEnabledServiceInfos(language).toMutableSet()
      result
    }
  }

  @Synchronized
  private fun <K, V> MutableMap<K, V>.computeIfAbsentSupportNull(
    key: K,
    mappingFunction: (K) -> V,
  ): V? {
    if (!containsKey(key)) {
      val value = mappingFunction(key)
      put(key, value)
    }
    return get(key)
  }

  fun getKey(keyId: Long?): KeyForMt? {
    return keys[keyId]
  }

  private val languageService: LanguageService by lazy {
    applicationContext.getBean(LanguageService::class.java)
  }

  private val projectService: ProjectService by lazy {
    applicationContext.getBean(ProjectService::class.java)
  }

  private val promptService: PromptService by lazy {
    applicationContext.getBean(PromptService::class.java)
  }

  private val mtServiceConfigService: MtServiceConfigService by lazy {
    applicationContext.getBean(MtServiceConfigService::class.java)
  }

  private val entityManger by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }

  private val metadataProvider by lazy {
    MetadataProvider(this)
  }

  val mtServiceManager: MtServiceManager by lazy {
    applicationContext.getBean(MtServiceManager::class.java)
  }
}
