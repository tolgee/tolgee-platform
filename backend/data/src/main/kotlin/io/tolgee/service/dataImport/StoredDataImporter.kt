package io.tolgee.service.dataImport

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.PropertyModification
import io.tolgee.api.IImportSettings
import io.tolgee.component.ActivityHolderProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.ImportResult
import io.tolgee.dtos.dataImport.SimpleImportConflictResult
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ImportConflictNotResolvedException
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.enums.ConflictType
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.translation.Translation
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import io.tolgee.service.dataImport.status.ImportApplicationStatus
import io.tolgee.service.dataImport.status.ImportApplicationStatusItem
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.key.TagService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.extractEntityId
import io.tolgee.util.flushAndClear
import io.tolgee.util.getSafeNamespace
import io.tolgee.util.nullIfEmpty
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import java.util.Collections
import java.util.IdentityHashMap

class StoredDataImporter(
  private val applicationContext: ApplicationContext,
  private val import: Import,
  private val forceMode: ForceMode = ForceMode.NO_FORCE,
  private val reportStatus: ((ImportApplicationStatusItem) -> Unit) = {},
  private val importSettings: IImportSettings,
  private val overrideMode: OverrideMode = OverrideMode.RECOMMENDED,
  private val errorOnUnresolvedConflict: Boolean? = null,
  // for single step import, we provide import data manager
  private val _importDataManager: ImportDataManager? = null,
  private val isSingleStepImport: Boolean = false,
  private val screenshots: List<ScreenshotImporter.Companion.ScreenshotToImport> = emptyList(),
  private val resolveConflict: ((ImportTranslation) -> ForceMode?)? = null,
) {
  private val logger = LoggerFactory.getLogger(StoredDataImporter::class.java)

  private val importDataManager by lazy {
    if (_importDataManager != null) {
      return@lazy _importDataManager
    }
    ImportDataManager(applicationContext, import)
  }

  private val screenshotImporter by lazy { ScreenshotImporter(applicationContext) }

  private val keyService = applicationContext.getBean(KeyService::class.java)
  private val namespaceService = applicationContext.getBean(NamespaceService::class.java)

  private val keyMetaService = applicationContext.getBean(KeyMetaService::class.java)

  private val securityService = applicationContext.getBean(SecurityService::class.java)

  private val imageUploadService = applicationContext.getBean(ImageUploadService::class.java)

  private val authenticationFacade = applicationContext.getBean(AuthenticationFacade::class.java)

  private val screenshotService = applicationContext.getBean(ScreenshotService::class.java)
  private val jdbcTemplate = applicationContext.getBean(JdbcTemplate::class.java)
  private val objectMapper = applicationContext.getBean(ObjectMapper::class.java)
  private val activityHolder by lazy {
    applicationContext.getBean(ActivityHolderProvider::class.java).getActivityHolder()
  }

  val translationsToSave = mutableListOf<Pair<ImportTranslation, Translation>>()

  /**
   * We need to persist data after everything is checked for resolved conflicts since
   * thrown ImportConflictNotResolvedException commits the transaction,
   * looking for key in this map is also faster than querying database
   */
  private val keysToSave = mutableMapOf<Pair<String?, String>, Key>()

  private val keyMetasToSave: MutableList<KeyMeta> = mutableListOf()

  /**
   * We need to persist data after everything is checked for resolved conflicts since
   * thrown ImportConflictNotResolvedException commits the transaction
   */
  private val translationService = applicationContext.getBean(TranslationService::class.java)

  private val entityManager = applicationContext.getBean(EntityManager::class.java)

  private val namespacesToSave = mutableMapOf<String?, Namespace>()

  // Identity-based set: Key entities are added here before persist (id = 0),
  // then checked with `in` after persist (id assigned). A regular HashSet would
  // fail because Key.hashCode() changes when the id is assigned by JPA.
  private val newKeys: MutableSet<Key> = Collections.newSetFromMap(IdentityHashMap())

  /**
   * Keys where base translation was changed, so we need to set outdated flag on all translations
   */
  val outdatedFlagKeys: MutableList<Long> = mutableListOf()

  /**
   * Id of one of the imported keys, captured during [saveKeysAndTranslationsInBatches]
   * so [publishOnProjectActivityEvent] can still construct the sentinel entry after
   * `keysToSave` has been drained.
   */
  private var sampleImportedKeyId: Long? = null

  val unresolvedConflicts: MutableList<ImportTranslation> = mutableListOf()

  /**
   * Lightweight records of base language translations collected during the batch
   * loop. Used to build [ActivityModifiedEntity] entries for [OnProjectActivityStoredEvent]
   * so [AutoTranslationEventHandler] can determine which keys need auto-translation.
   */
  private data class BaseTranslationRecord(
    val translationId: Long,
    val keyId: Long,
    val languageId: Long,
    val text: String?,
  )

  private val baseLanguageTranslations = mutableListOf<BaseTranslationRecord>()

  /**
   * These are all keyMetas from all the keys to import. If multiple keys have same name and
   * namespace, the meta is merged to one, so there is only one meta for one key!!!
   *
   * It can be used only when we are finally importing the data. Before that, we cannot merge it.
   */
  private val storedMetas: MutableMap<Pair<String?, String>, KeyMeta> by lazy {
    val result: MutableMap<Pair<String?, String>, KeyMeta> = mutableMapOf()
    val metasToMerge =
      if (isSingleStepImport) {
        importDataManager.storedKeys.map { it.value.keyMeta }.filterNotNull()
      } else {
        keyMetaService.getWithFetchedData(this.import)
      }

    metasToMerge.forEach { currentKeyMeta ->
      val mapKey = currentKeyMeta.importKey!!.file.namespace to currentKeyMeta.importKey!!.name
      result[mapKey] = result[mapKey]?.let { existingKeyMeta ->
        keyMetaService.import(existingKeyMeta, currentKeyMeta)
        existingKeyMeta
      } ?: currentKeyMeta
    }
    result
  }

  fun doImport(): ImportResult {
    val importStart = System.currentTimeMillis()

    fun elapsed() = System.currentTimeMillis() - importStart

    reportStatus(ImportApplicationStatusItem(ImportApplicationStatus.PREPARING_AND_VALIDATING))

    importDataManager.storedLanguages.forEach {
      it.prepareImport()
    }
    logger.trace("Import phase: prepareImport completed in {}ms", elapsed())

    throwOnUnresolvedConflicts(unresolvedConflicts)

    checkTranslationPermissions()

    addKeysAndCheckPermissions()

    handleKeyMetas()
    logger.trace("Import phase: addKeys + handleKeyMetas completed in {}ms", elapsed())

    reportStatus(ImportApplicationStatusItem(ImportApplicationStatus.STORING_KEYS))

    namespaceService.saveAll(namespacesToSave.values)

    addScreenshots()

    handlePluralization()
    logger.trace("Import phase: namespaces + screenshots + pluralization completed in {}ms", elapsed())

    reportStatus(
      ImportApplicationStatusItem(
        ImportApplicationStatus.STORING_TRANSLATIONS,
        totalKeys = keysToSave.size,
        importedKeys = 0,
      ),
    )

    // Disable the Hibernate activity interceptor and record activity manually
    // via JDBC to avoid O(n²) overhead from the interceptor accumulating
    // ActivityModifiedEntity objects outside the Hibernate session.
    activityHolder.enableAutoCompletion = false

    // Release import entities (ImportKey, ImportTranslation) from importDataManager.
    // At 1M keys × 3 languages, these hold ~6M entities (~1.5 GB).
    releaseImportData()
    logger.trace("Import phase: releaseImportData completed in {}ms", elapsed())

    saveKeysAndTranslationsInBatches()
    logger.trace("Import phase: saveKeysAndTranslationsInBatches completed in {}ms", elapsed())

    reportStatus(ImportApplicationStatusItem(ImportApplicationStatus.FINALIZING))

    translationService.setOutdatedBatch(outdatedFlagKeys)

    tagNewKeys()

    entityManager.flush()

    deleteOtherKeys()
    logger.trace("Import phase: finalization completed in {}ms", elapsed())

    // Publish OnProjectActivityEvent manually since enableAutoCompletion = false
    // bypasses the BeforeTransactionCompletionProcess that normally fires it.
    publishOnProjectActivityEvent()

    entityManager.flushAndClear()

    // Build minimal modifiedEntities so AutoTranslationEventHandler can identify
    // which keys need auto-translation. Only base language translations with
    // changed text are included — the handler checks these to create a batch job.
    val revision = activityHolder.activityRevision
    baseLanguageTranslations.forEach { record ->
      revision.modifiedEntities.add(
        ActivityModifiedEntity(revision, "Translation", record.translationId).apply {
          describingRelations =
            mapOf(
              "key" to EntityDescriptionRef("Key", record.keyId),
              "language" to EntityDescriptionRef("Language", record.languageId),
            )
          modifications = mutableMapOf("text" to PropertyModification(old = null, new = record.text))
        },
      )
    }

    applicationContext.publishEvent(
      OnProjectActivityStoredEvent(this, revision),
    )

    return ImportResult(
      unresolvedConflicts.nullIfEmpty()?.let {
        getUnresolvedConflicts(unresolvedConflicts)
      },
    )
  }

  private fun addScreenshots() {
    screenshotImporter.importScreenshots(
      screenshots,
      existingKeys = importDataManager.existingKeys.values.toList(),
      newKeys = newKeys.toMutableList(),
      addKeyToSave = { namespace: String?, key: String ->
        this.addKeyToSave(namespace, key)
      },
      projectId = import.project.id,
    )
  }

  /**
   * Throws error when `errorOnUnresolvedConflict` is set on the API request
   * and there are some unresolved conflicts
   */
  private fun throwOnUnresolvedConflicts(unresolvedConflicts: List<ImportTranslation>) {
    // when force mode is `NO_FORCE` default value is true, otherwise false
    val shouldThrowError =
      errorOnUnresolvedConflict ?: when (forceMode) {
        ForceMode.NO_FORCE -> true
        else -> false
      }

    if (unresolvedConflicts.isNotEmpty() && shouldThrowError) {
      throw ImportConflictNotResolvedException(params = getUnresolvedConflicts(unresolvedConflicts))
    }
  }

  private fun tagNewKeys() {
    (importSettings as? SingleStepImportRequest)?.tagNewKeys?.let { tagNewKeys ->
      tagService.tagKeys(newKeys.associateWith { tagNewKeys })
    }
  }

  private fun deleteOtherKeys() {
    if ((importSettings as? SingleStepImportRequest)?.removeOtherKeys == true) {
      val namespaces = import.files.map { getSafeNamespace(it.namespace) }.toSet()
      val existingKeys =
        importDataManager.existingKeys.entries.filter {
          // taking only keys from namespaces that are included in the import
          namespaces.contains(getSafeNamespace(it.value.namespace?.name))
        }
      val importedKeys = importDataManager.storedKeys.entries.map { (pair) -> Pair(pair.first.namespace, pair.second) }
      val otherKeys = existingKeys.filter { existing -> !importedKeys.contains(existing.key) }
      if (otherKeys.isNotEmpty()) {
        keyService.hardDeleteMultiple(otherKeys.map { it.value.id })
      }
    }
  }

  private fun handlePluralization() {
    val handler = PluralizationHandler(importDataManager, this, translationService)
    handler.handlePluralization()
    saveKeys(handler.keysToSave)
  }

  private fun saveKeyMetaDataForBatch(keyBatch: Collection<Key>) {
    val batchKeySet = keyBatch.toSet()
    val batchMetas = keyMetasToSave.filter { it.key in batchKeySet }
    if (batchMetas.isEmpty()) return

    val comments = keyBatch.flatMap { it.keyMeta?.comments ?: emptyList() }
    val codeReferences = keyBatch.flatMap { it.keyMeta?.codeReferences ?: emptyList() }
    keyMetaService.saveAll(batchMetas)
    keyMetaService.saveAllComments(comments)
    keyMetaService.saveAllCodeReferences(codeReferences)

    comments.groupBy { it.keyMeta }.forEach { (keyMeta, c) -> keyMeta.comments = c.toMutableList() }
    codeReferences.groupBy { it.keyMeta }.forEach { (keyMeta, c) -> keyMeta.codeReferences = c.toMutableList() }
  }

  private fun saveKeyMetaData(keyEntitiesToSave: Collection<Key>) {
    // hibernate bug workaround:
    // saving key metas will cause them to be recreated by hibernate with empty values
    // we have to save references to comments and codeReferences before saving key metas
    val comments =
      keyEntitiesToSave.flatMap {
        it.keyMeta?.comments ?: emptyList()
      }
    val codeReferences =
      keyEntitiesToSave.flatMap {
        it.keyMeta?.codeReferences ?: emptyList()
      }
    keyMetaService.saveAll(keyMetasToSave)
    keyMetaService.saveAllComments(comments)
    keyMetaService.saveAllCodeReferences(codeReferences)

    // set links to comments and code references to point to correct (previous)
    // instances instead of the new empty ones
    comments.groupBy { it.keyMeta }.forEach { (keyMeta, comments) ->
      keyMeta.comments = comments.toMutableList()
    }
    codeReferences.groupBy { it.keyMeta }.forEach { (keyMeta, codeReferences) ->
      keyMeta.codeReferences = codeReferences.toMutableList()
    }
  }

  private fun saveTranslations() {
    checkTranslationPermissions()
    translationService.saveAll(translationsToSave.map { it.second })
  }

  private fun getUnresolvedConflicts(conflicts: List<ImportTranslation>): List<SimpleImportConflictResult> {
    return conflicts.map {
      val conflict = it.conflict ?: throw IllegalStateException("Unresolved conflict should have conflict data")
      SimpleImportConflictResult(
        keyName = it.key.name,
        keyNamespace = conflict.key.namespace?.name,
        language = conflict.language.tag,
        isOverridable = ConflictType.isOverridable(it.conflictType),
      )
    }
  }

  /**
   * Releases import entities from [importDataManager] and clears the Hibernate session.
   *
   * At 1M keys × 3 languages, [ImportDataManager] holds ~3M [ImportKey], ~3M [ImportTranslation],
   * plus existing keys/translations loaded for conflict resolution — totalling ~1.5 GB of heap.
   * By the time we reach [saveKeysAndTranslationsInBatches], all data has been extracted into
   * [keysToSave] and [translationsToSave], so the import entities are no longer needed.
   *
   * Clearing the collections AND flushing the Hibernate session lets the GC reclaim both
   * the Java objects and the Hibernate persistence context entries that track them (~2 GB
   * of [MutableEntityEntry], [EntityKey], [LazyAttributeLoadingInterceptor] etc.).
   */
  private fun releaseImportData() {
    importDataManager.storedKeys.clear()
    importDataManager.storedTranslations.clear()
    importDataManager.translationsToUpdateDueToCollisions.clear()
  }

  /**
   * Saves keys, their metadata, and translations in batches with flushAndClear
   * after each batch. This prevents the Hibernate session from holding all entities
   * simultaneously, which would cause OOM at large scale (100k+ keys).
   *
   * Memory discipline: at million-key scale, holding all 1M Key + 3M Translation
   * entities alive for the entire loop (through `keysToSave`, `translationsToSave`,
   * `translationsByKey`) pushes the heap over 8 GB. To avoid this, we drain
   * `keysToSave` and `translationsByKey` as we process each batch so the GC can
   * reclaim already-persisted entities while the loop is still running.
   *
   * Keys and their translations are saved in the same batch so key references
   * remain managed — no getReference() proxies needed.
   */
  private fun saveKeysAndTranslationsInBatches() {
    // Group by key identity (reference), not by equals/hashCode which is slow for entities.
    val translationsByKey = IdentityHashMap<Key, MutableList<Translation>>()
    translationsToSave.forEach { (_, translation) ->
      translationsByKey.getOrPut(translation.key) { mutableListOf() }.add(translation)
    }
    // We've captured everything we need in translationsByKey — release the
    // source list now so its entries can be collected as we drain the map.
    translationsToSave.clear()

    val totalKeyCount = keysToSave.size
    val totalBatches = (totalKeyCount + FLUSH_BATCH_SIZE - 1) / FLUSH_BATCH_SIZE
    var savedTranslations = 0

    // Drain keysToSave via its iterator so processed entries are removed as
    // we go. keyBatch is reused across iterations to avoid allocation churn.
    val keyIterator = keysToSave.entries.iterator()
    val keyBatch = ArrayList<Key>(FLUSH_BATCH_SIZE)
    var batchIndex = 0
    var activityRecorder: ImportActivityRecorder? = null
    while (keyIterator.hasNext()) {
      keyBatch.clear()
      var filled = 0
      while (keyIterator.hasNext() && filled < FLUSH_BATCH_SIZE) {
        keyBatch.add(keyIterator.next().value)
        keyIterator.remove()
        filled++
      }

      // Prevent interceptor from accumulating activity — we record via JDBC
      keyBatch.forEach { it.disableActivityLogging = true }

      keyService.saveAll(keyBatch)
      saveKeyMetaDataForBatch(keyBatch)

      // Capture one imported id for the post-import OnProjectActivityEvent sentinel,
      // since keysToSave is drained as we go and will be empty by the time the
      // event is published.
      if (sampleImportedKeyId == null) {
        sampleImportedKeyId = keyBatch.firstOrNull()?.id
      }

      val batchTranslations = ArrayList<Translation>(keyBatch.size * 4)
      keyBatch.forEach { key ->
        // remove() releases the map's reference to the translation list once
        // we move it into batchTranslations, so finished batches can be GC'd.
        val list = translationsByKey.remove(key) ?: return@forEach
        list.forEach { it.disableActivityLogging = true }
        batchTranslations.addAll(list)
      }
      translationService.saveAll(batchTranslations)
      savedTranslations += batchTranslations.size

      // Collect base language translation records for auto-translation event.
      val baseLanguage = import.project.baseLanguage
      if (baseLanguage != null) {
        batchTranslations.forEach {
          if (it.language.id == baseLanguage.id && !it.text.isNullOrBlank()) {
            baseLanguageTranslations.add(BaseTranslationRecord(it.id, it.key.id, it.language.id, it.text))
          }
        }
      }

      // Create the activity revision after the first batch is flushed.
      // This must happen after keys/translations are persisted because
      // entityManager.flush() would fail with TransientObjectException if
      // unsaved Key entities exist in the session.
      if (activityRecorder == null) {
        val activityRevision = activityHolder.activityRevision
        if (activityRevision.id == 0L) {
          activityRevision.type = ActivityType.IMPORT
          activityRevision.projectId = import.project.id
          activityRevision.authorId = import.author.id
          entityManager.persist(activityRevision)
        }
      }

      entityManager.flushAndClear()

      if (activityRecorder == null) {
        val activityRevision = activityHolder.activityRevision
        entityManager.detach(activityRevision)
        activityRecorder = ImportActivityRecorder(jdbcTemplate, objectMapper, activityRevision.id, null)
      }

      // Record activity via JDBC (no interceptor overhead).
      // Only record newly created keys (not pre-existing ones from the project).
      val newKeyBatch = keyBatch.filter { it in newKeys }
      val recorder = activityRecorder!!
      recorder.recordKeys(newKeyBatch)
      val batchKeyIds = keyBatch.mapTo(HashSet()) { it.id }
      recorder.recordKeyMetas(keyMetasToSave.filter { it.key?.id in batchKeyIds })
      recorder.recordTranslations(batchTranslations)
      recorder.recordDescribingEntities(keyBatch, batchTranslations)

      batchIndex++
      val importedKeys = (batchIndex * FLUSH_BATCH_SIZE).coerceAtMost(totalKeyCount)
      reportStatus(
        ImportApplicationStatusItem(
          ImportApplicationStatus.STORING_TRANSLATIONS,
          totalKeys = totalKeyCount,
          importedKeys = importedKeys,
        ),
      )
      logger.trace(
        "Batch {}/{}: {} keys, {} translations (total: {})",
        batchIndex,
        totalBatches,
        keyBatch.size,
        batchTranslations.size,
        savedTranslations,
      )
    }
  }

  /**
   * Manually publishes [OnProjectActivityEvent] with a sentinel `modifiedEntities` map.
   *
   * Background: with [activityHolder.enableAutoCompletion] = false we skip the normal
   * `BeforeTransactionCompletionProcess` path that publishes this event. We still need
   * downstream listeners to react to the import:
   *  - [io.tolgee.ee.component.branching.BranchRevisionListener] bumps `branch.revision`
   *    via a single bulk JPQL update — driven off any modified entity with `branchId` set.
   *  - [io.tolgee.component.eventListeners.LanguageStatsListener] refreshes language
   *    stats — triggered when `modifiedEntities` contains `Key`/`Translation`/`Language`
   *    and resolves the affected branches via the entity ids it finds.
   *  - [io.tolgee.ee.component.EeKeyCountReportingListener] reports key-count usage —
   *    triggered when `modifiedEntities` contains `Key`.
   *
   * Instead of recreating the full per-entity activity map (which would defeat the
   * purpose of manual JDBC recording), we publish a single sentinel `Key` entry whose
   * `branchId` is the import branch. This is enough to trigger all three listeners
   * with the correct branch context. The activity rows themselves are already in the
   * DB at this point, written via [ImportActivityRecorder].
   */
  private fun publishOnProjectActivityEvent() {
    val sampleKeyId = sampleImportedKeyId ?: return
    val sentinel =
      ActivityModifiedEntity(
        activityHolder.activityRevision,
        "Key",
        sampleKeyId,
      ).apply { branchId = extractEntityId(import.branch) }

    val sentinelEntities: ModifiedEntitiesType =
      mutableMapOf(Key::class to mutableMapOf(sampleKeyId to sentinel))

    applicationContext.publishEvent(
      OnProjectActivityEvent(
        activityHolder.activityRevision,
        sentinelEntities,
        activityHolder.utmData,
        activityHolder.businessEventData,
      ),
    )
  }

  companion object {
    private const val FLUSH_BATCH_SIZE = 5000
  }

  private fun saveKeys(): Collection<Key> {
    return saveKeys(keysToSave.values)
  }

  private fun saveKeys(keys: Collection<Key>): Collection<Key> {
    keyService.saveAll(keys)
    return keys
  }

  private fun addKeysAndCheckPermissions() {
    addAllKeys()
    checkKeyPermissions()
  }

  private fun checkTranslationPermissions() {
    val langIds = translationsToSave.mapTo(mutableSetOf()) { it.second.language.id }
    securityService.checkLanguageTranslatePermission(import.project.id, langIds.toList())
  }

  private fun checkKeyPermissions() {
    val isCreatingKey = keysToSave.values.any { it.id == 0L }
    if (isCreatingKey) {
      securityService.checkProjectPermission(import.project.id, Scope.KEYS_CREATE)
    }
  }

  private fun handleKeyMetas() {
    this.importDataManager.storedKeys.entries.forEach { (fileNamePair, importKey) ->
      if (!importKey.shouldBeImported) {
        return@forEach
      }
      val importedKeyMeta = storedMetas[fileNamePair.first.namespace to importKey.name]
      // don't touch key meta when imported key has no meta
      if (importedKeyMeta != null) {
        keysToSave[fileNamePair.first.namespace to importKey.name]?.let { newKey ->
          // if key is obtained or created and meta exists, take it and import the data from the imported one
          // persist is cascaded on key, so it should be fine
          val keyMeta =
            importDataManager.existingMetas[fileNamePair.first.namespace to importKey.name]?.also {
              keyMetaService.import(it, importedKeyMeta, importSettings.overrideKeyDescriptions)
            } ?: importedKeyMeta
          // also set key and remove import key
          keyMeta.also {
            it.key = newKey
            it.importKey = null
          }
          // assign new meta
          newKey.keyMeta = keyMeta
          keyMetasToSave.add(keyMeta)
        }
      }
    }
  }

  private fun addAllKeys() {
    importDataManager.storedKeys.map { (_, importKey) ->
      if (!importKey.shouldBeImported) {
        return@map
      }
      addKeyToSave(importKey.file.namespace, importKey.name)
    }
  }

  private fun ImportLanguage.prepareImport() {
    importDataManager.populateStoredTranslations(this)
    importDataManager.handleConflicts(true)
    importDataManager.applyKeyCreateChange(importSettings.createNewKeys)
    importDataManager.getStoredTranslations(this).forEach { it.doImport() }
  }

  private fun ImportTranslation.doImport() {
    if (!this.isSelectedToImport || !this.key.shouldBeImported || this.language.ignored) {
      return
    }

    val resolution = this.getConflictResolution()

    if (resolution == ForceMode.KEEP) {
      return
    }

    if (resolution == ForceMode.NO_FORCE) {
      unresolvedConflicts.add(this)
      return
    }

    val language =
      this.language.existingLanguage
        ?: throw BadRequestException(Message.EXISTING_LANGUAGE_NOT_SELECTED)
    val translation =
      this.conflict ?: Translation().apply {
        this.language = language
      }

    translation.key = existingKey
    if (language == language.project.baseLanguage && translation.text != this.text) {
      outdatedFlagKeys.add(translation.key.id)
    }
    translationService.setTranslationTextNoSave(translation, text)
    translationsToSave.add(this to translation)
  }

  private val ImportTranslation.existingKey: Key
    get() {
      // get key from already saved keys to save
      return keysToSave.computeIfAbsent(this.key.file.namespace to this.key.name) {
        // or get it from conflict or create new one
        val newKey =
          importDataManager.existingKeys[this.key.file.namespace to this.key.name]
            ?: createNewKey(this.key.name, this.key.file.namespace)
        newKey
      }
    }

  private fun addKeyToSave(
    namespace: String?,
    keyName: String,
  ): Key {
    return keysToSave.computeIfAbsent(namespace to keyName) {
      importDataManager.existingKeys[namespace to keyName] ?: createNewKey(keyName, namespace)
    }
  }

  private fun createNewKey(
    name: String,
    namespace: String?,
  ): Key {
    return Key(name = name).apply {
      project = import.project
      this.namespace = getNamespace(namespace)
      this.branch = import.branch
      newKeys.add(this)
    }
  }

  private fun ImportTranslation.getConflictResolution(): ForceMode {
    if (this.conflict == null) {
      return ForceMode.OVERRIDE
    }

    if (this.resolved) {
      return if (this.override) ForceMode.OVERRIDE else ForceMode.KEEP
    }
    val currentForceMode = resolveConflict?.invoke(this) ?: forceMode
    if (currentForceMode == ForceMode.KEEP) {
      return ForceMode.KEEP
    }
    if (currentForceMode == ForceMode.OVERRIDE) {
      if (
        (overrideMode == OverrideMode.ALL) &&
        ConflictType.isOverridable(this.conflictType)
      ) {
        return ForceMode.OVERRIDE
      }
      if (
        (overrideMode == OverrideMode.RECOMMENDED) &&
        ConflictType.isOverridableAndRecommended(this.conflictType)
      ) {
        return ForceMode.OVERRIDE
      }
    }
    return ForceMode.NO_FORCE
  }

  private fun getNamespace(name: String?): Namespace? {
    name ?: return null
    return importDataManager.existingNamespaces[name] ?: namespacesToSave.computeIfAbsent(name) {
      Namespace(name, import.project)
    }
  }

  private val tagService by lazy {
    applicationContext.getBean(TagService::class.java)
  }
}
