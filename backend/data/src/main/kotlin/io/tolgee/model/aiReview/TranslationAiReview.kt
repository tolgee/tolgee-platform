package io.tolgee.model.aiReview

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.Type
import java.util.Date

@Entity
@Table(
  indexes = [
    Index(columnList = "status"),
    Index(columnList = "rule_id"),
    Index(columnList = "batch_job_id"),
    Index(columnList = "manual_run_batch_job_id"),
  ],
)
class TranslationAiReview(
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "translation_id", nullable = false, unique = true)
  @OnDelete(action = OnDeleteAction.CASCADE)
  var translation: Translation,
) : StandardAuditModel() {
  @Column(nullable = false)
  var textHash: String = ""

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var status: AiReviewStatus = AiReviewStatus.PENDING

  var score: Int? = null

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var issues: MutableList<AiReviewIssue> = mutableListOf()

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rule_id")
  @OnDelete(action = OnDeleteAction.SET_NULL)
  var rule: AiReviewRule? = null

  @Enumerated(EnumType.STRING)
  var ruleMode: AiReviewRuleMode? = null

  var threshold: Int? = null

  var promptId: Long? = null

  var promptName: String? = null

  var promptRevision: Int? = null

  var providerName: String? = null

  var model: String? = null

  @Enumerated(EnumType.STRING)
  var failureReason: AiReviewFailureReason? = null

  @Enumerated(EnumType.STRING)
  var lastFailureReason: AiReviewFailureReason? = null

  @Temporal(TemporalType.TIMESTAMP)
  var lastFailedAt: Date? = null

  @ColumnDefault("false")
  var reReviewRequested: Boolean = false

  var manualRunBatchJobId: Long? = null

  var batchJobId: Long? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resolved_by_id")
  @OnDelete(action = OnDeleteAction.SET_NULL)
  var resolvedBy: UserAccount? = null

  @Temporal(TemporalType.TIMESTAMP)
  var resolvedAt: Date? = null

  @Temporal(TemporalType.TIMESTAMP)
  var reviewedAt: Date? = null
}
