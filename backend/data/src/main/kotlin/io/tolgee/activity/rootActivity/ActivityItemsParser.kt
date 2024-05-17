package io.tolgee.activity.rootActivity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component

@Component
class ActivityItemsParser(
  private val objectMapper: ObjectMapper,
) {
  fun parse(items: List<Array<Any?>>): List<ActivityTreeResultItem> {
    return items.map {
      parseItem(it)
    }
  }

  private fun parseItem(item: Array<Any?>): ActivityTreeResultItem {
    val modifications = item[2].parseJsonOrNull()
    val type = Type.getByValue(item[4] as String)
    return ActivityTreeResultItem(
      entityClass = item[0] as String,
      description = getDescription(modifications, item),
      modifications = modifications,
      entityId = item[3] as Long,
      type = type,
      parentId = getParentId(item),
    )
  }

  private fun getParentId(item: Array<Any?>): Long? {
    return try {
      item[5] as? Long
    } catch (e: IndexOutOfBoundsException) {
      null
    }
  }

  private fun getDescription(
    modifications: Map<String, Any?>?,
    item: Array<Any?>,
  ): Map<String, Any?> {
    val description = item[1].parseJsonOrNull()?.toMutableMap() ?: mutableMapOf()

    val new = getNewFromModifications(modifications) ?: return description

    return description + new
  }

  private fun getNewFromModifications(modifications: Map<String, Any?>?): Map<String, Any?>? {
    return modifications?.map {
      @Suppress("UNCHECKED_CAST")
      it.key to ((it.value as? Map<String, Any?>)?.get("new"))
    }?.toMap()
  }

  private fun Any?.parseJsonOrNull(): Map<String, Any?>? {
    if (this is String) {
      return try {
        objectMapper.readValue<Map<String, Any?>>(this)
      } catch (e: Exception) {
        null
      }
    }
    return null
  }

  enum class Type(val value: String) {
    MODIFIED_ENTITY("AME"),
    DESCRIBING_ENTITY("ADE"),
    ;

    companion object {
      fun getByValue(value: String): Type {
        return entries.first { it.value == value }
      }
    }
  }
}
