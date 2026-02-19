package io.tolgee.model.qa

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
@Table(
  indexes = [
    Index(columnList = "translation_id"),
    Index(columnList = "state"),
  ],
)
class TranslationQaIssue(
  @Enumerated(EnumType.STRING)
  var type: QaCheckType = QaCheckType.EMPTY_TRANSLATION,
  @Enumerated(EnumType.STRING)
  var message: QaIssueMessage = QaIssueMessage.QA_EMPTY_TRANSLATION,
  @Column(columnDefinition = "text")
  var replacement: String? = null,
  var positionStart: Int = 0,
  var positionEnd: Int = 0,
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'OPEN'")
  var state: QaIssueState = QaIssueState.OPEN,
  @Column(columnDefinition = "text")
  var params: String? = null,
  @ManyToOne
  var translation: Translation,
) : StandardAuditModel()
