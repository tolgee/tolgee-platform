package io.tolgee.model.aiReview

import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.Language
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(paths = ["rule", "language"])
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["rule_id", "language_id"]),
  ],
)
class AiReviewRuleLanguageOverride(
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "rule_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  var rule: AiReviewRule,
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "language_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  var language: Language,
  @ActivityLoggedProp
  var threshold: Int = 85,
  @ColumnDefault("false")
  @ActivityLoggedProp
  var never: Boolean = false,
) : StandardAuditModel()
