package io.tolgee.model.aiReview

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.Project
import io.tolgee.model.Prompt
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.Type

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "prompt_id"),
  ],
)
class AiReviewRule(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  var project: Project,
  var name: String? = null,
  @ColumnDefault("true")
  @Column(nullable = false)
  var enabled: Boolean = true,
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'DRY_RUN'")
  @Column(nullable = false)
  var mode: AiReviewRuleMode = AiReviewRuleMode.DRY_RUN,
  @ColumnDefault("85")
  @Column(nullable = false)
  var threshold: Int = 85,
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var languageThresholds: MutableList<AiReviewLanguageThreshold> = mutableListOf(),
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.SET_NULL)
  var prompt: Prompt? = null,
  @ColumnDefault("true")
  @Column(nullable = false)
  var skipWithOpenQaIssues: Boolean = true,
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var tagIn: MutableList<String> = mutableListOf(),
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var tagNotIn: MutableList<String> = mutableListOf(),
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var namespaceIn: MutableList<String> = mutableListOf(),
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var namespaceNotIn: MutableList<String> = mutableListOf(),
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var languageIds: MutableList<Long> = mutableListOf(),
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var branchIn: MutableList<String> = mutableListOf(),
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  var branchNotIn: MutableList<String> = mutableListOf(),
) : StandardAuditModel()
