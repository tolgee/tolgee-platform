package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.Language_
import io.tolgee.model.Project_
import io.tolgee.model.Screenshot
import io.tolgee.model.Screenshot_
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace_
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference_
import io.tolgee.model.keyBigMeta.KeysDistance
import io.tolgee.model.keyBigMeta.KeysDistance_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment_
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import io.tolgee.security.authentication.AuthenticationFacade
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.ListJoin
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery

class QueryBase<T>(
  private val cb: CriteriaBuilder,
  private val projectId: Long,
  val query: CriteriaQuery<T>,
  private val languages: Set<LanguageDto>,
  params: TranslationFilters,
  private var isKeyIdsQuery: Boolean = false,
  private val entityManager: EntityManager,
  private val authenticationFacade: AuthenticationFacade,
) {
  val whereConditions: MutableSet<Predicate> = HashSet()
  val root: Root<Key> = query.from(Key::class.java)
  val keyNameExpression: Path<String> = root.get(Key_.name)
  val keyIsPluralExpression: Path<Boolean> = root.get(Key_.isPlural)
  val keyArgNameExpression: Path<String?> = root.get(Key_.pluralArgName)
  val keyIdExpression: Path<Long> = root.get(Key_.id)
  val querySelection = QuerySelection()
  val fullTextFields: MutableSet<Expression<String>> = HashSet()
  lateinit var namespaceNameExpression: Path<String>
  var translationsTextFields: MutableSet<Expression<String>> = HashSet()
  lateinit var screenshotCountExpression: Expression<Long>
  val groupByExpressions: MutableSet<Expression<*>> = mutableSetOf()
  private val queryGlobalFiltering = QueryGlobalFiltering(params, this, cb, entityManager)
  var queryTranslationFiltering = QueryTranslationFiltering(params, this, cb)

  init {
    querySelection[KeyWithTranslationsView::keyId.name] = keyIdExpression
    querySelection[KeyWithTranslationsView::keyName.name] = keyNameExpression
    querySelection[KeyWithTranslationsView::keyIsPlural.name] = keyIsPluralExpression
    querySelection[KeyWithTranslationsView::keyPluralArgName.name] = keyArgNameExpression
    whereConditions.add(cb.equal(root.get<Any>(Key_.PROJECT).get<Any>(Project_.ID), this.projectId))
    fullTextFields.add(root.get(Key_.name))
    addLeftJoinedColumns()
    queryGlobalFiltering.apply()
  }

  private fun addLeftJoinedColumns() {
    addNamespace()
    addDescription()
    addScreenshotCounts()
    addContextCounts()
    addLanguageSpecificFields()
  }

  private fun addLanguageSpecificFields() {
    val outdatedFieldMap = mutableMapOf<String, Expression<Boolean>>()

    for (language in languages) {
      val translation = addTranslationId(language)
      val translationTextField = addTranslationText(translation, language)
      this.fullTextFields.add(translationTextField)
      translationsTextFields.add(translationTextField)

      val translationStateField = addTranslationStateField(translation, language)
      queryTranslationFiltering.apply(language, translationTextField, translationStateField)

      val outdatedField = addTranslationOutdatedField(translation, language)
      outdatedFieldMap[language.tag] = outdatedField

      addNotFilteringTranslationFields(language, translation)
      addComments(translation, language)
    }

    queryTranslationFiltering.apply(outdatedFieldMap)
  }

  private fun addTranslationOutdatedField(
    translation: ListJoin<Key, Translation>,
    language: LanguageDto,
  ): Path<Boolean> {
    val translationOutdated = translation.get(Translation_.outdated)
    this.querySelection[language to TranslationView::outdated] = translationOutdated
    return translationOutdated
  }

  private fun addComments(
    translation: ListJoin<Key, Translation>,
    language: LanguageDto,
  ) {
    val commentsJoin = translation.join(Translation_.comments, JoinType.LEFT)
    val commentsExpression = cb.countDistinct(commentsJoin)
    this.querySelection[language to TranslationView::commentCount] = commentsExpression

    val unresolvedCommentsJoin = translation.join(Translation_.comments, JoinType.LEFT)
    unresolvedCommentsJoin.on(
      cb.equal(unresolvedCommentsJoin.get(TranslationComment_.state), TranslationCommentState.NEEDS_RESOLUTION),
    )

    val unresolvedCommentsExpression = cb.countDistinct(unresolvedCommentsJoin)
    this.querySelection[language to TranslationView::unresolvedCommentCount] = unresolvedCommentsExpression
  }

  private fun addTranslationStateField(
    translation: ListJoin<Key, Translation>,
    language: LanguageDto,
  ): Path<TranslationState> {
    val translationStateField = translation.get(Translation_.state)
    this.querySelection[language to TranslationView::state] = translationStateField
    return translationStateField
  }

  private fun addTranslationText(
    translation: ListJoin<Key, Translation>,
    language: LanguageDto,
  ): Path<String> {
    val translationTextField = translation.get(Translation_.text)
    this.querySelection[language to TranslationView::text] = translationTextField
    return translationTextField
  }

  private fun addTranslationId(language: LanguageDto): ListJoin<Key, Translation> {
    val translation = this.root.join(Key_.translations, JoinType.LEFT)
    translation.on(cb.equal(translation.get(Translation_.language).get(Language_.id), language.id))
    val translationId = translation.get(Translation_.id)
    this.querySelection[language to TranslationView::id] = translationId
    groupByExpressions.add(translationId)
    return translation
  }

  private fun addScreenshotCounts() {
    val screenshotSubquery = this.query.subquery(Long::class.java)
    val screenshotRoot = screenshotSubquery.from(Screenshot::class.java)
    val screenshotCount = cb.count(screenshotRoot.get(Screenshot_.id))
    screenshotSubquery.select(screenshotCount)
    screenshotSubquery.where(screenshotRoot.get(Screenshot_.id).`in`(getScreenshotIdFilterSubquery()))
    screenshotCountExpression = screenshotSubquery.selection
    this.querySelection[KeyWithTranslationsView::screenshotCount.name] = screenshotCountExpression
  }

  private fun getScreenshotIdFilterSubquery(): Subquery<Long> {
    val subquery = this.query.subquery(Long::class.java)
    val subQueryRoot = subquery.from(Key::class.java)
    val keyScreenshotReference = subQueryRoot.join(Key_.keyScreenshotReferences)
    subquery.where(cb.equal(subQueryRoot.get(Key_.id), this.root.get(Key_.id)))
    return subquery.select(keyScreenshotReference.get(KeyScreenshotReference_.screenshot).get(Screenshot_.id))
  }

  private fun addNotFilteringTranslationFields(
    language: LanguageDto,
    translation: ListJoin<Key, Translation>,
  ) {
    if (!isKeyIdsQuery) {
      this.querySelection[language to TranslationView::auto] = translation.get(Translation_.auto)
      this.querySelection[language to TranslationView::mtProvider] = translation.get(Translation_.mtProvider)
    }
  }

  private fun addNamespace() {
    val namespace = this.root.join(Key_.namespace, JoinType.LEFT)
    val namespaceId = namespace.get(Namespace_.id)
    val namespaceName = namespace.get(Namespace_.name)
    namespaceNameExpression = namespaceName
    this.querySelection[KeyWithTranslationsView::keyNamespaceId.name] = namespaceId
    this.querySelection[KeyWithTranslationsView::keyNamespace.name] = namespaceName
    this.fullTextFields.add(namespaceName)
    groupByExpressions.add(namespaceId)
    groupByExpressions.add(namespaceName)
  }

  private fun addDescription() {
    val keyMeta = this.root.join(Key_.keyMeta, JoinType.LEFT)
    val description = keyMeta.get(KeyMeta_.description)
    this.querySelection[KeyWithTranslationsView::keyDescription.name] = description
    this.fullTextFields.add(description)
    groupByExpressions.add(description)
  }

  private fun addContextCounts() {
    val contextSubquery = this.query.subquery(Long::class.java)
    val contextRoot = contextSubquery.from(KeysDistance::class.java)
    contextSubquery.select(contextRoot.get(KeysDistance_.key1Id))
    contextSubquery.where(
      cb.or(
        cb.equal(this.root.get(Key_.id), contextRoot.get(KeysDistance_.key1Id)),
        cb.equal(this.root.get(Key_.id), contextRoot.get(KeysDistance_.key2Id)),
      ),
    )
    this.querySelection[KeyWithTranslationsView::contextPresent.name] = cb.exists(contextSubquery)
  }

  val Expression<String>.isNotNullOrBlank: Predicate
    get() = cb.and(cb.isNotNull(this), cb.notEqual(this, ""))

  val Expression<String>.isNullOrBlank: Predicate
    get() = cb.or(cb.isNull(this), cb.equal(this, ""))
}
