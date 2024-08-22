package io.tolgee.activity.groups.matchers.modifiedEntity

import org.jooq.Field
import org.jooq.JSON

/**
 * This is all the context that can be used when data are queried from the database
 */
class SqlContext(
  val modificationsField: Field<JSON>,
  var entityClassField: Field<String>,
  var revisionTypeField: Field<Int>,
)
