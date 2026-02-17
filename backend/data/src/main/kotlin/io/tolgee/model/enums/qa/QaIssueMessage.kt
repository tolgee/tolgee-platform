package io.tolgee.model.enums.qa

import com.fasterxml.jackson.annotation.JsonValue
import java.util.Locale

enum class QaIssueMessage {
  QA_EMPTY_TRANSLATION,
  ;

  @JsonValue
  fun code(): String = name.lowercase(Locale.getDefault())
}
