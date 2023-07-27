package io.tolgee.model.batch.params

import io.tolgee.model.StandardAuditModel

class UntagKeysParams : StandardAuditModel() {
  var tags: List<String> = listOf()
}
