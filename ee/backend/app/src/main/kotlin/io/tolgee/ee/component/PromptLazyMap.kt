package io.tolgee.ee.component

import com.github.jknack.handlebars.Handlebars
import io.tolgee.ee.data.prompt.PromptVariableDto
import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.PromptVariableType

class PromptLazyMap : AbstractMap<String, Any?>() {
  private lateinit var internalMap: Map<String, Variable>

  fun setMap(map: Map<String, Variable>) {
    internalMap = map
  }

  override fun get(key: String): Any? {
    val promptValue = internalMap.get(key)

    if (!promptValue?.props.isNullOrEmpty()) {
      val mapParams =
        promptValue.props.associateBy { it.name }

      val lazyMap = PromptLazyMap()
      lazyMap.setMap(mapParams)
      return lazyMap
    }

    val stringValue = promptValue?.lazyValue?.let { it() } ?: promptValue?.value
    return stringValue?.let { if (it is String) Handlebars.SafeString(it) else it }
  }

  override val entries: Set<Map.Entry<String, Any?>>
    get() {
      return internalMap.entries.map { (key) -> Entry(key) { get(key) } }.toSet()
    }

  companion object {
    private class Entry(
      override val key: String,
      val valGetter: () -> Any?,
    ) : Map.Entry<String, Any?> {
      override val value: Any?
        get() = valGetter()
    }

    class Variable(
      val name: String,
      var value: Any? = null,
      var lazyValue: (() -> Any?)? = null,
      val description: String? = null,
      val props: MutableList<Variable> = mutableListOf(),
      val type: PromptVariableType? = null,
      val option: BasicPromptOption? = null,
    ) {
      fun toPromptVariableDto(): PromptVariableDto {
        val computedType =
          if (props.isNotEmpty()) {
            PromptVariableType.OBJECT
          } else {
            type ?: PromptVariableType.STRING
          }

        return PromptVariableDto(
          name = name,
          description = description,
          value = value?.toString(),
          props =
            if (props.isNotEmpty()) {
              props.map { it.toPromptVariableDto() }.toMutableList()
            } else {
              null
            },
          type = computedType,
        )
      }
    }
  }
}
