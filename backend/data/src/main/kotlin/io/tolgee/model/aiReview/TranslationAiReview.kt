package io.tolgee.model.aiReview

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.Prompt
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.translation.Translation
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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
    Index(columnList = "rule_id"),
  ],
)
class TranslationAiReview(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(unique = true, nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  var translation: Translation,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var outcome: AiReviewOutcome = AiReviewOutcome.PENDING,
  @Column(nullable = false, length = 32)
  var textHash: String = "",
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.SET_NULL)
  var rule: AiReviewRule? = null,
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.SET_NULL)
  var prompt: Prompt? = null,
  var providerName: String? = null,
  var score: Int? = null,
  var threshold: Int? = null,
  @ColumnDefault("false")
  @Column(nullable = false)
  var dryRun: Boolean = false,
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  var issues: MutableList<AiReviewIssue>? = null,
  var errorCode: String? = null,
  @Column(columnDefinition = "text")
  var errorDetail: String? = null,
  @Temporal(TemporalType.TIMESTAMP)
  var reviewedAt: Date? = null,
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.SET_NULL)
  var resolvedBy: UserAccount? = null,
  @Temporal(TemporalType.TIMESTAMP)
  var resolvedAt: Date? = null,
  @Temporal(TemporalType.TIMESTAMP)
  var claimedAt: Date? = null,
  @Enumerated(EnumType.STRING)
  var previousOutcome: AiReviewOutcome? = null,
  var runBatchJobId: Long? = null,
) : StandardAuditModel()
