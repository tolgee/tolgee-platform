package io.tolgee.model

import java.util.*

interface SoftDeletable {
  var deletedAt: Date?
}
