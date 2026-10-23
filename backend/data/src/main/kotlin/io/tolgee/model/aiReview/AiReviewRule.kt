package io.tolgee.model.aiReview

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.propChangesProvider.EntityWithIdCollectionPropChangesProvider
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Prompt
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.Type

@Entity
@ActivityLoggedEntity
@Table(
  indexes = [
    Index(columnList = "project_id"),
  ],
)
class AiReviewRule(
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  var project: Project,
) : StandardAuditModel() {
  @ActivityLoggedProp
  @ActivityDescribingProp
  var name: String? = null

  @ActivityLoggedProp
  @ColumnDefault("true")
  var enabled: Boolean = true

  @ActivityLoggedProp
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var tagIn: MutableList<String> = mutableListOf()

  @ActivityLoggedProp
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var tagNotIn: MutableList<String> = mutableListOf()

  @ActivityLoggedProp
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var namespaceIn: MutableList<String> = mutableListOf()

  @ActivityLoggedProp
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var namespaceNotIn: MutableList<String> = mutableListOf()

  @ActivityLoggedProp(EntityWithIdCollectionPropChangesProvider::class)
  @ManyToMany
  @JoinTable(
    name = "ai_review_rule_language",
    joinColumns = [JoinColumn(name = "rule_id")],
    inverseJoinColumns = [JoinColumn(name = "language_id")],
  )
  var languages: MutableSet<Language> = mutableSetOf()

  @ActivityLoggedProp
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var branchIn: MutableList<String> = mutableListOf()

  @ActivityLoggedProp
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var branchNotIn: MutableList<String> = mutableListOf()

  @ActivityLoggedProp
  @ColumnDefault("85")
  var threshold: Int = 85

  @ActivityLoggedProp
  @ColumnDefault("false")
  var never: Boolean = false

  @ActivityLoggedProp
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var mode: AiReviewRuleMode = AiReviewRuleMode.DRY_RUN

  @ActivityLoggedProp
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prompt_id")
  @OnDelete(action = OnDeleteAction.SET_NULL)
  var prompt: Prompt? = null

  @ActivityLoggedProp
  @ColumnDefault("true")
  var skipOpenQaIssues: Boolean = true

  @OneToMany(mappedBy = "rule", cascade = [CascadeType.ALL], orphanRemoval = true)
  var overrides: MutableList<AiReviewRuleLanguageOverride> = mutableListOf()
}
