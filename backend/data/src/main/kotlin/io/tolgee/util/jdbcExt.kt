package io.tolgee.util

import io.tolgee.configuration.JsonbObjectMapperSupplier
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.sql.Connection
import java.sql.ResultSet

private const val FETCH_SIZE = 10_000

@PublishedApi
internal val jsonbObjectMapper: ObjectMapper by lazy { JsonbObjectMapperSupplier().get() }

/**
 * PostgreSQL honours the fetch size only inside a transaction. With autocommit on, the driver reads the whole
 * result into memory before the first row is handled.
 */
fun Connection.forEachRow(
  sql: String,
  params: LongArray,
  handle: (ResultSet) -> Unit,
) {
  prepareStatement(sql).use { statement ->
    statement.fetchSize = FETCH_SIZE
    params.forEachIndexed { index, value -> statement.setLong(index + 1, value) }
    statement.executeQuery().use { rows ->
      while (rows.next()) handle(rows)
    }
  }
}

fun ResultSet.getLongOrNull(column: String): Long? = getObject(column) as Long?

inline fun <reified T> ResultSet.getJson(column: String): T? =
  getString(column)?.let { jsonbObjectMapper.readValue<T?>(it) }

inline fun <reified T> ResultSet.getJsonSet(column: String): MutableSet<T> {
  val json = getString(column)
  if (json == null || json == "[]" || json == "null") return mutableSetOf()
  return jsonbObjectMapper.readValue<MutableSet<T>>(json)
}
