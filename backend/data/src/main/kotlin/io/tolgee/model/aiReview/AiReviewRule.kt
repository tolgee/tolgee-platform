package io.tolgee.model.aiReview

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
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
  var name: String? = null

  @ColumnDefault("true")
  var enabled: Boolean = true

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var tagIn: MutableList<String> = mutableListOf()

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var tagNotIn: MutableList<String> = mutableListOf()

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var namespaceIn: MutableList<String> = mutableListOf()

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var namespaceNotIn: MutableList<String> = mutableListOf()

  @ManyToMany
  @JoinTable(
    name = "ai_review_rule_language",
    joinColumns = [JoinColumn(name = "rule_id")],
    inverseJoinColumns = [JoinColumn(name = "language_id")],
  )
  var languages: MutableSet<Language> = mutableSetOf()

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var branchIn: MutableList<String> = mutableListOf()

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var branchNotIn: MutableList<String> = mutableListOf()

  @ColumnDefault("85")
  var threshold: Int = 85

  @ColumnDefault("false")
  var never: Boolean = false

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var mode: AiReviewRuleMode = AiReviewRuleMode.DRY_RUN

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prompt_id")
  @OnDelete(action = OnDeleteAction.SET_NULL)
  var prompt: Prompt? = null

  @ColumnDefault("true")
  var skipOpenQaIssues: Boolean = true

  @OneToMany(mappedBy = "rule", cascade = [CascadeType.ALL], orphanRemoval = true)
  var overrides: MutableList<AiReviewRuleLanguageOverride> = mutableListOf()
}
