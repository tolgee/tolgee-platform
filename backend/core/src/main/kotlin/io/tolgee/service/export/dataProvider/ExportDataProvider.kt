package io.tolgee.service.export.dataProvider

import io.tolgee.dtos.IExportParams
import io.tolgee.model.Language
import io.tolgee.model.Language_
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Namespace_
import io.tolgee.model.key.Tag
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.service.key.TaggedKeyIdsProvider
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.ListJoin
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.SetJoin
import org.springframework.context.ApplicationContext

class ExportDataProvider(
  private val applicationContext: ApplicationContext,
  private val exportParams: IExportParams,
  private val projectId: Long,
  private val overrideLanguageTag: List<String>? = null,
) {
  private val cb: CriteriaBuilder by lazy { entityManager.criteriaBuilder }
  val query: CriteriaQuery<ExportDataView> by lazy {
    cb.createQuery(ExportDataView::class.java)
  }

  private val key: Root<Key> by lazy {
    query.from(Key::class.java)
  }

  private lateinit var tagJoin: SetJoin<KeyMeta, Tag>
  private lateinit var keyMetaJoin: Join<Key, KeyMeta>
  private lateinit var translationJoin: ListJoin<Key, Translation>
  private lateinit var languageJoin: SetJoin<Project, Language>
  private lateinit var projectJoin: Join<Key, Project>
  private lateinit var namespaceJoin: Join<Key, Namespace>

  private var whereConditions: MutableList<Predicate> = mutableListOf()

  private fun initQuery() {
    addJoins()
    addWhereConditions()
    addSelect()
  }

  private fun addJoins() {
    projectJoin = key.join(Key_.project)
    namespaceJoin = key.join(Key_.namespace, JoinType.LEFT)
    languageJoin = joinLanguage()
    translationJoin = joinTranslation(key, languageJoin)
    keyMetaJoin = key.join(Key_.keyMeta, JoinType.LEFT)
    tagJoin = keyMetaJoin.join(KeyMeta_.tags, JoinType.LEFT)
  }

  private fun addSelect() {
    query.multiselect(
      key.get(Key_.id),
      key.get(Key_.name),
      keyMetaJoin.get(KeyMeta_.custom),
      keyMetaJoin.get(KeyMeta_.description),
      namespaceJoin.get(Namespace_.name),
      key.get(Key_.isPlural),
      languageJoin.get(Language_.id),
      languageJoin.get(Language_.tag),
      translationJoin.get(Translation_.id),
      translationJoin.get(Translation_.text),
      translationJoin.get(Translation_.state),
    )
  }

  private fun addWhereConditions() {
    filterProjectId()
    filterKeyId()
    applyTagFilters()
    filterKeyIdNot()
    filterKeyPrefix()
    filterState()
    filterNamespaces()

    query.where(*whereConditions.toTypedArray())
    query.orderBy(cb.asc(key.get(Key_.name)))
  }

  private fun filterProjectId() {
    whereConditions.add(cb.equal(projectJoin.get(Project_.id), projectId))
  }

  private fun applyTagFilters() {
    if (shouldApplyTagFilters) {
      whereConditions.add(key.get(Key_.id).`in`(tagFilteredKeyIds))
    }
  }

  private fun filterKeyId() {
    if (exportParams.filterKeyId != null) {
      whereConditions.add(key.get(Key_.id).`in`(exportParams.filterKeyId))
    }
  }

  private fun filterKeyIdNot() {
    if (exportParams.filterKeyIdNot != null) {
      whereConditions.add(cb.not(key.get(Key_.id).`in`(exportParams.filterKeyIdNot)))
    }
  }

  private fun filterKeyPrefix() {
    if (exportParams.filterKeyPrefix != null) {
      whereConditions.add(cb.like(key.get(Key_.name), "${exportParams.filterKeyPrefix}%"))
    }
  }

  private fun filterState() {
    if (exportParams.filterState != null) {
      var condition = translationJoin.get(Translation_.state).`in`(exportParams.filterState)

      if (exportParams.shouldContainUntranslated) {
        condition = cb.or(condition, cb.isNull(translationJoin))
      }

      whereConditions.add(condition)
    }
  }

  private fun filterNamespaces() {
    val filterNamespace = exportParams.filterNamespace
    if (!filterNamespace.isNullOrEmpty()) {
      val hasNull = filterNamespace.contains(null) || filterNamespace.contains("")

      val withoutNull = filterNamespace.filterNotNull()
      val namespaceName = namespaceJoin.get(Namespace_.name)
      var condition = namespaceName.`in`(withoutNull)
      if (hasNull) {
        condition = cb.or(condition, namespaceName.isNull)
      }
      whereConditions.add(condition)
    }
  }

  private fun joinTranslation(
    key: Root<Key>,
    language: SetJoin<Project, Language>,
  ): ListJoin<Key, Translation> {
    val translation = key.join(Key_.translations, JoinType.LEFT)
    translation.on(
      cb.equal(language, translation.get(Translation_.language)),
    )
    return translation
  }

  private fun joinLanguage(): SetJoin<Project, Language> {
    val language = projectJoin.join(Project_.languages)

    val languageTagsToFilter = overrideLanguageTag ?: exportParams.languages
    if (languageTagsToFilter != null) {
      language.on(language.get(Language_.tag).`in`(languageTagsToFilter))
    }
    return language
  }

  val data: List<ExportTranslationView>
    by lazy {
      initQuery()
      val resultList = entityManager.createQuery(query).resultList
      val keyMap = transformResult(resultList)
      keyMap.flatMap { it.value.translations.values }
    }

  private fun transformResult(resultList: MutableList<ExportDataView>): HashMap<Long, ExportKeyView> {
    val keyMap = LinkedHashMap<Long, ExportKeyView>()
    resultList.forEach { dataView ->
      val keyView =
        keyMap.computeIfAbsent(dataView.keyId) {
          @Suppress("UNCHECKED_CAST")
          ExportKeyView(
            dataView.keyId,
            dataView.keyName,
            dataView.keyCustom as? Map<String, Any?>?,
            dataView.keyDescription,
            dataView.namespace,
            dataView.keyIsPlural,
          )
        }

      keyView.translations[dataView.languageTag] =
        ExportTranslationView(
          id = dataView.translationId,
          text = dataView.translationText,
          state = dataView.translationState ?: TranslationState.UNTRANSLATED,
          key = keyView,
          languageTag = dataView.languageTag,
          description = dataView.keyDescription,
        )
    }
    return keyMap
  }

  private val shouldApplyTagFilters
    get() =
      exportParams.filterTagIn != null || exportParams.filterTagNotIn != null || exportParams.filterTag != null

  private val tagFilteredKeyIds by lazy {
    taggedKeyIdsProvider.getFilteredKeys(
      projectId,
      getJoinedTagInFilter(),
      exportParams.filterTagNotIn,
    )
  }

  private fun getJoinedTagInFilter(): MutableList<String>? {
    if (exportParams.filterTagIn == null && exportParams.filterTag == null) {
      return null
    }

    val tagIn = (exportParams.filterTagIn ?: emptyList()).toMutableList()

    if (exportParams.filterTag != null) {
      tagIn.add(exportParams.filterTag!!)
    }

    return tagIn
  }

  private val entityManager: EntityManager
    by lazy { applicationContext.getBean(EntityManager::class.java) }

  private val taggedKeyIdsProvider: TaggedKeyIdsProvider
    by lazy { applicationContext.getBean(TaggedKeyIdsProvider::class.java) }
}
