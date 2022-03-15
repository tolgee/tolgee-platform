package io.tolgee.service.query_builders

import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.query_results.TranslationHistoryView
import io.tolgee.model.Revision_
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.service.UserAccountService
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.hibernate.envers.query.AuditQuery
import org.hibernate.envers.query.projection.AuditProjection
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import javax.persistence.EntityManager

open class TranslationHistoryProvider(
  applicationContext: ApplicationContext,
  private val translationId: Long,
  private val pageable: Pageable
) {

  private val entityManager: EntityManager = applicationContext.getBean(EntityManager::class.java)

  private val auditReader = AuditReaderFactory.get(entityManager)

  private val baseQuery: AuditQuery
    get() = auditReader
      .createQuery()
      .forRevisionsOfEntity(Translation::class.java, false, true)

  private val projections: MutableList<Projection<*>> = mutableListOf()

  private val userAccountService: UserAccountService = applicationContext.getBean(UserAccountService::class.java)

  fun getHistory(): PageImpl<TranslationHistoryView> {
    val q = getDataQuery()
    val rawData = q.resultList
    val viewData = parseData(rawData)
    viewData.addUserData()
    return PageImpl<TranslationHistoryView>(viewData, pageable, getCount())
  }

  private fun List<TranslationHistoryView>.addUserData() {
    val ids = this.asSequence().map { it.authorId }.filterNotNull().toSet()
    val accounts = userAccountService.getAllByIds(ids).associateBy { it.id }
    this.forEach { viewItem ->
      accounts[viewItem.authorId]?.let { userAccount ->
        viewItem.authorEmail = userAccount.username
        viewItem.authorName = userAccount.name
        viewItem.authorAvatarHash = userAccount.avatarHash
      }
    }
  }

  private fun parseData(
    rawData: MutableList<Any?>,
  ) = rawData.map {
    @Suppress("UNCHECKED_CAST")
    it as Array<Any>
    val item = TranslationHistoryView()
    it.zip(projections).forEach { (value, projection) ->
      @Suppress("UNCHECKED_CAST")
      projection as Projection<Any>
      projection.assigner(value, item)
    }
    item
  }

  private fun getDataQuery(): AuditQuery {
    val q = baseQuery
      .addProjection<RevisionType>(AuditEntity.revisionType()) { value, item ->
        item.revisionType = value
      }
      .addProjection<String?>(AuditEntity.property(Translation_.TEXT)) { value, item ->
        item.text = value
      }
      .addProjection<TranslationState?>(AuditEntity.property(Translation_.STATE)) { value, item ->
        item.state = value
      }
      .addProjection<Boolean?>(AuditEntity.property(Translation_.AUTO)) { value, item ->
        item.auto = value
      }
      .addProjection<MtServiceType?>(AuditEntity.property(Translation_.MT_PROVIDER)) { value, item ->
        item.mtProvider = value
      }
      .addProjection<Long>(AuditEntity.revisionProperty(Revision_.TIMESTAMP)) { value, item ->
        item.timestamp = value
      }
      .addProjection<Long?>(AuditEntity.revisionProperty(Revision_.AUTHOR_ID)) { value, item ->
        item.authorId = value
      }.setFirstResult(pageable.offset.toInt())
      .setMaxResults(pageable.pageSize)
    q.addTranslateIdRestriction()
    q.addOrder(AuditEntity.revisionNumber().desc())
    return q
  }

  fun AuditQuery.addTranslateIdRestriction() {
    this.add(AuditEntity.id().eq(translationId))
  }

  private fun <T> AuditQuery.addProjection(
    auditProjection: AuditProjection,
    assigner: (value: T, item: TranslationHistoryView) -> Unit
  ): AuditQuery {
    val projection = Projection(
      auditProjection = auditProjection,
      assigner = assigner
    )
    projections.add(projection)
    this.addProjection(projection.auditProjection)
    return this
  }

  private fun getCount(): Long {
    val q = baseQuery.addProjection(AuditEntity.id().count())
    q.addTranslateIdRestriction()
    return q.singleResult as Long
  }

  class Projection<T>(
    val auditProjection: AuditProjection,
    val assigner: (value: T, item: TranslationHistoryView) -> Unit
  )
}
