package io.tolgee.model.batch.params

import io.tolgee.model.StandardAuditModel

class TagKeysParams : StandardAuditModel() {
  var tags: List<String> = listOf()
}
