package io.tolgee.model.qa

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.translation.Translation
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(
  indexes = [
    Index(columnList = "translation_id"),
    Index(columnList = "state"),
  ],
)
class TranslationQaIssue(
  @Enumerated
  var type: QaCheckType = QaCheckType.EMPTY_TRANSLATION,
  @Enumerated
  var message: QaIssueMessage = QaIssueMessage.QA_EMPTY_TRANSLATION,
  @Column(columnDefinition = "text")
  var replacement: String? = null,
  var positionStart: Int = 0,
  var positionEnd: Int = 0,
  @Enumerated
  @ColumnDefault("0")
  var state: QaIssueState = QaIssueState.OPEN,
  @ManyToOne
  var translation: Translation,
) : StandardAuditModel()
