package io.tolgee.model.aiReview

import io.tolgee.model.Project
import io.tolgee.model.enums.aiReview.SourceChangeAction
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
class AiReviewProjectSettings(
  @Id
  var projectId: Long = 0,
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "project_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  var project: Project? = null,
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'RETRANSLATE'")
  @Column(nullable = false)
  var onSourceChangeAiApproved: SourceChangeAction = SourceChangeAction.RETRANSLATE,
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'KEEP'")
  @Column(nullable = false)
  var onSourceChangeHumanEdited: SourceChangeAction = SourceChangeAction.KEEP,
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'KEEP'")
  @Column(nullable = false)
  var onSourceChangeHumanReviewed: SourceChangeAction = SourceChangeAction.KEEP,
) {
  val isDefault: Boolean
    get() =
      onSourceChangeAiApproved == SourceChangeAction.RETRANSLATE &&
        onSourceChangeHumanEdited == SourceChangeAction.KEEP &&
        onSourceChangeHumanReviewed == SourceChangeAction.KEEP
}
