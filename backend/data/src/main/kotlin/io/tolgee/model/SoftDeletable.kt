package io.tolgee.model

import java.util.Date

interface SoftDeletable {
  var deletedAt: Date?
}
