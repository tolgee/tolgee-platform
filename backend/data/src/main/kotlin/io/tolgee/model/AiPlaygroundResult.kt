package io.tolgee.model

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.enums.PromptType
import io.tolgee.model.key.Key
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "key_id"),
    Index(columnList = "language_id"),
    Index(columnList = "user_id"),
  ],
)
class AiPlaygroundResult(
  @ManyToOne
  var project: Project,
  @ManyToOne
  var key: Key? = null,
  @ManyToOne
  var language: Language? = null,
  @ManyToOne
  var user: UserAccount? = null,
  @Column(columnDefinition = "text")
  var translation: String? = null,
  var contextDescription: String? = null,
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'TRANSLATE'")
  @Column(nullable = false)
  var type: PromptType = PromptType.TRANSLATE,
  var reviewScore: Int? = null,
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  var reviewIssues: MutableList<AiReviewIssue>? = null,
  var reviewError: String? = null,
) : StandardAuditModel()
