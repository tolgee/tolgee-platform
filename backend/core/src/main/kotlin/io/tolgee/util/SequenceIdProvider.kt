package io.tolgee.util

import java.sql.Connection

class SequenceIdProvider(
  private val sequenceName: String,
  private val allocationSize: Int,
) {
  private var currentId: Long? = null
  private var currentMaxId: Long? = null

  fun next(connection: Connection): Long {
    allocateIfRequired(connection)
    val currentId = currentId
    this.currentId = currentId!! + 1
    return currentId
  }

  private fun allocateIfRequired(connection: Connection) {
    if (currentId == null || currentMaxId == null || currentId!! >= currentMaxId!!) {
      allocate(connection)
    }
  }

  private fun allocate(connection: Connection) {
    @Suppress("SqlSourceToSinkFlow")
    val statement = connection.prepareStatement("select nextval('$sequenceName')")
    val resultSet = statement.executeQuery()
    resultSet.next()
    currentMaxId = resultSet.getLong(1)
    currentId = currentMaxId!! - allocationSize + 1
  }
}
