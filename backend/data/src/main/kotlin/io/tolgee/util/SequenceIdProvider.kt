package io.tolgee.util

import java.sql.Connection

class SequenceIdProvider(
  private val connection: Connection,
  private val sequenceName: String,
  private val allocationSize: Int
) {

  private var currentId: Long? = null
  private var currentMaxId: Long? = null

  fun next(): Long {
    allocateIfRequired()
    val currentId = currentId
    this.currentId = currentId!! + 1
    return currentId
  }

  private fun allocateIfRequired() {
    if (currentId == null || currentMaxId == null || currentId!! >= currentMaxId!!) {
      allocate()
    }
  }

  private fun allocate() {
    @Suppress("SqlSourceToSinkFlow")
    val statement = connection.prepareStatement("select nextval('$sequenceName')")
    val resultSet = statement.executeQuery()
    resultSet.next()
    currentId = resultSet.getLong(1)
    currentMaxId = currentId!! + allocationSize - 1
  }
}
