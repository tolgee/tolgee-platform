package io.tolgee.model.qa

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.translation.Translation
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault

@Entity
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(paths = ["translation"])
@Table(
  indexes = [
    Index(columnList = "translation_id"),
  ],
)
class TranslationQaIssue(
  @Enumerated(EnumType.STRING)
  @ActivityDescribingProp
  var type: QaCheckType = QaCheckType.EMPTY_TRANSLATION,
  @Enumerated(EnumType.STRING)
  var message: QaIssueMessage = QaIssueMessage.QA_EMPTY_TRANSLATION,
  @Column(columnDefinition = "text")
  var replacement: String? = null,
  var positionStart: Int? = null,
  var positionEnd: Int? = null,
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'OPEN'")
  @ActivityLoggedProp
  var state: QaIssueState = QaIssueState.OPEN,
  @Column(columnDefinition = "text")
  var params: String? = null,
  @ColumnDefault("false")
  var virtual: Boolean = false,
  var pluralVariant: String? = null,
  @ManyToOne
  var translation: Translation,
) : StandardAuditModel()
