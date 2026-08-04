package io.tolgee.configuration

import io.hypersistence.utils.hibernate.type.util.JsonSerializer
import io.hypersistence.utils.hibernate.type.util.ObjectMapperWrapper
import org.hibernate.internal.util.SerializationHelper
import org.hibernate.type.SerializationException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.type.TypeFactory
import java.io.Serializable
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.util.AbstractMap

/** JSON-round-trip clone for non-[Serializable] JSONB attributes; registered via the `hypersistence.utils.json.serializer` property. */
@Suppress("UNCHECKED_CAST")
class JsonCloneSerializer : JsonSerializer {
  private val objectMapperWrapper get() = ObjectMapperWrapper.INSTANCE

  override fun <T> clone(jsonObject: T): T {
    if (jsonObject == null) return jsonObject
    when (jsonObject) {
      is String -> return jsonObject
      is Collection<*> -> {
        val commonElementType = findCommonElementType(jsonObject)
        if (commonElementType != null && !Serializable::class.java.isAssignableFrom(commonElementType)) {
          val type: Type =
            TypeFactory.createDefaultInstance().constructParametricType(jsonObject.javaClass, commonElementType)
          return objectMapperWrapper.fromBytes(objectMapperWrapper.toBytes(jsonObject), type)
        }
      }
      is Map<*, *> -> {
        val commonElementType = findCommonElementType(jsonObject)
        if (commonElementType != null) {
          val commonKeyClass = commonElementType.key
          val commonValueClass = commonElementType.value
          val keyCoreOrNotSerializable =
            isCoreJavaType(commonKeyClass) || !Serializable::class.java.isAssignableFrom(commonKeyClass)
          val valueCoreOrNotSerializable =
            isCoreJavaType(commonValueClass) || !Serializable::class.java.isAssignableFrom(commonValueClass)
          if ((keyCoreOrNotSerializable || valueCoreOrNotSerializable) &&
            jsonObject.javaClass.typeParameters.size == 2
          ) {
            val type: Type =
              TypeFactory
                .createDefaultInstance()
                .constructParametricType(jsonObject.javaClass, commonKeyClass, commonValueClass)
            return objectMapperWrapper.fromBytes(objectMapperWrapper.toBytes(jsonObject), type)
          }
        }
      }
      is JsonNode -> return jsonObject.deepCopy() as T
    }

    if (jsonObject is Serializable) {
      try {
        return SerializationHelper.clone(jsonObject) as T
      } catch (e: SerializationException) {
        // Serializable wrapper, non-serializable contents — expected, not an error.
      }
    }
    return jsonClone(jsonObject)
  }

  private fun isCoreJavaType(type: Class<*>): Boolean {
    val typePackage = type.getPackage()
    return typePackage != null && typePackage.name.startsWith("java")
  }

  private fun findCommonElementType(collection: Collection<*>): Class<*>? {
    var commonElementType: Class<*>? = null
    for (element in collection) {
      if (element == null) continue
      if (commonElementType == null) {
        commonElementType = element.javaClass
        continue
      }
      commonElementType = resolveCommonElementType(commonElementType, element.javaClass) ?: return null
    }
    return commonElementType
  }

  private fun resolveCommonElementType(
    commonElementType: Class<*>,
    elementClass: Class<*>,
  ): Class<*>? {
    if (commonElementType.isAssignableFrom(elementClass) && !Modifier.isAbstract(commonElementType.modifiers)) {
      return commonElementType
    }
    val superclass = commonElementType.superclass
    if (superclass == null || superclass == Any::class.java) return null
    return resolveCommonElementType(superclass, elementClass)
  }

  private fun findCommonElementType(map: Map<*, *>): Map.Entry<Class<*>, Class<*>>? {
    var commonElementType: Map.Entry<Class<*>, Class<*>>? = null
    for (entry in map.entries) {
      val key = entry.key
      val value = entry.value
      if (key == null || value == null) continue
      val elementClass: Map.Entry<Class<*>, Class<*>> = AbstractMap.SimpleEntry(key.javaClass, value.javaClass)
      if (commonElementType == null) {
        commonElementType = elementClass
        continue
      }
      commonElementType = resolveCommonElementType(commonElementType, elementClass) ?: return null
    }
    return commonElementType
  }

  private fun resolveCommonElementType(
    commonElementType: Map.Entry<Class<*>, Class<*>>,
    elementClass: Map.Entry<Class<*>, Class<*>>,
  ): Map.Entry<Class<*>, Class<*>>? {
    val commonKeyClass = commonElementType.key
    val commonValueClass = commonElementType.value
    if (commonKeyClass.isAssignableFrom(elementClass.key) && !isAbstractType(commonKeyClass) &&
      commonValueClass.isAssignableFrom(elementClass.value) && !isAbstractType(commonValueClass)
    ) {
      return commonElementType
    }
    val keySuperclass = nextSuperclass(commonKeyClass, elementClass.key)
    val valueSuperclass = nextSuperclass(commonValueClass, elementClass.value)
    if (keySuperclass != null && keySuperclass != Any::class.java && keySuperclass != commonKeyClass &&
      valueSuperclass != null && valueSuperclass != Any::class.java && valueSuperclass != commonValueClass
    ) {
      return resolveCommonElementType(AbstractMap.SimpleEntry(keySuperclass, valueSuperclass), elementClass)
    }
    return null
  }

  private fun nextSuperclass(
    current: Class<*>,
    target: Class<*>,
  ): Class<*>? {
    if (current == target) return current
    return current.superclass
  }

  private fun isAbstractType(type: Class<*>): Boolean = Modifier.isAbstract(type.modifiers) && !type.isArray

  private fun <T> jsonClone(jsonObject: T): T =
    objectMapperWrapper.fromBytes(objectMapperWrapper.toBytes(jsonObject), (jsonObject as Any).javaClass) as T
}
