package io.tolgee.api.v2.controllers.configurationProps

import io.tolgee.configuration.annotations.AdditionalDocsProperties
import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class ConfigurationDocumentationProvider {
  private val globalItems: MutableList<DocItem> = mutableListOf()
  val docs by lazy {
    globalItems + listOf(handleObject(TolgeeProperties(), null, null))
  }

  private fun handleObject(
    obj: Any,
    parent: KProperty<*>?,
    annotation: DocProperty?,
    isList: Boolean = false,
  ): Group {
    val additionalProps =
      obj::class
        .findAnnotations(AdditionalDocsProperties::class)
        .buildPropertiesTree()
        .sortedByGroupAndName()

    val objDef = obj::class.findAnnotations(DocProperty::class).singleOrNull()
    val confPropsDef = obj::class.findAnnotations(ConfigurationProperties::class).singleOrNull()
    val props =
      obj::class
        .declaredMemberProperties
        .mapNotNull {
          handleProperty(it, obj)
        }.sortedByGroupAndName()

    // Sort is stable - additional props will be added at the end, but props not part of a group will be moved before groups
    // Docs renderer expects groups to be always at the end of the list
    val allProps = (props + additionalProps).sortedByGroup()

    val name =
      annotation?.name?.nullIfEmpty ?: objDef?.name?.nullIfEmpty ?: parent?.name ?: confPropsDef
        ?.prefix
        ?.replace(
          "(.*)\\.(.+?)\$".toRegex(),
          "$1",
        )?.nullIfEmpty
        ?: throw RuntimeException("No name for $obj with parent $parent")
    return Group(
      name = name,
      displayName = annotation?.displayName?.nullIfEmpty ?: objDef?.displayName?.nullIfEmpty,
      description = annotation?.description?.nullIfEmpty ?: objDef?.description?.nullIfEmpty,
      children = allProps,
      prefix = annotation?.prefix?.nullIfEmpty ?: objDef?.prefix?.nullIfEmpty ?: confPropsDef?.prefix?.nullIfEmpty,
      isList = isList,
    )
  }

  private fun handleProperty(
    it: KProperty1<out Any, *>,
    obj: Any,
  ): DocItem? {
    val annotation =
      it.javaField?.getAnnotation(DocProperty::class.java)
        ?: it.findAnnotations<DocProperty>().singleOrNull()
    if (annotation?.hidden == true) {
      return null
    }
    val returnTypeFirstArgument =
      it.returnType.arguments
        .firstOrNull()
        ?.type

    when {
      it.returnType.isSubtypeOf(typeOf<List<*>?>()) && returnTypeFirstArgument?.isPrimitive() == false -> {
        // we expect this is a list of some configuration property classes, so we need to dig deeper
        val items = it.getter.call(obj)
        val child =
          (items as? List<*>)?.firstOrNull()
            ?: try {
              // if the list is empty, we try to instantiate an object ourselves
              returnTypeFirstArgument.instantiate()
            } catch (e: Exception) {
              throw RuntimeException("Property ${it.name} failed to instantiate", e)
            }

        return handleObject(child, it, annotation, isList = true)
      }

      it.returnType.isPrimitive() -> {
        // For these simple types, we cannot dig deeper, so we store them as a property
        val name = getPropertyName(annotation, it)
        return Property(
          name = name,
          displayName = annotation?.displayName?.nullIfEmpty,
          description = annotation?.description?.nullIfEmpty,
          defaultValue = getDefaultValue(annotation, obj, it).nullIfEmpty,
          defaultExplanation = annotation?.defaultExplanation?.nullIfEmpty,
          removedIn = annotation?.removedIn?.nullIfEmpty,
        )
      }

      else -> {
        // we expect this is some configuration property class, so we need to dig deeper and examine the object instance
        val child =
          it.getter.call(obj)
            ?: throw RuntimeException("Property ${it.name} is null")
        return handleObject(child, it, annotation)
      }
    }
  }

  private fun KType.isPrimitive(): Boolean {
    return this.isSubtypeOfPrimitiveType() || (this.javaType as? Class<*>)?.isEnum == true
  }

  private fun KType.isSubtypeOfOneOf(vararg types: KType): Boolean {
    types.forEach {
      if (this.isSubtypeOf(it)) {
        return true
      }
    }
    return false
  }

  private fun KType.isSubtypeOfPrimitiveType(): Boolean {
    return isSubtypeOfOneOf(
      typeOf<String?>(),
      typeOf<Int?>(),
      typeOf<Long?>(),
      typeOf<Double?>(),
      typeOf<List<*>?>(),
      typeOf<Boolean?>(),
      typeOf<Map<*, *>?>(),
    )
  }

  private fun KType.instantiate(): Any {
    val kClass = jvmErasure

    return try {
      // Works if there’s a true no-arg constructor OR all constructor params are optional.
      kClass.createInstance()
    } catch (e1: Exception) {
      // Fallback to raw Java no-arg.
      try {
        val ctor = kClass.java.getDeclaredConstructor()
        ctor.newInstance()
      } catch (e2: Exception) {
        e2.addSuppressed(e1)
        throw RuntimeException("Could not instantiate $kClass", e2)
      }
    }
  }

  private fun List<AdditionalDocsProperties>.buildPropertiesTree(): List<DocItem> =
    buildList {
      this@buildPropertiesTree.forEach {
        if (it.global) {
          it.properties.forEach {
            globalItems.add(getPropertyTree(it))
          }
          return@forEach
        }
        it.properties.forEach {
          add(getPropertyTree(it))
        }
      }
    }

  private fun List<DocItem>.sortedByGroup(): List<DocItem> {
    return this.sortedBy(
      { it is Group },
    )
  }

  private fun List<DocItem>.sortedByGroupAndName(): List<DocItem> {
    return this.sortedWith(
      compareBy(
        { it is Group },
        { it.name },
      ),
    )
  }

  private fun getPropertyName(
    annotation: DocProperty?,
    it: KProperty1<out Any, *>,
  ) = annotation?.name?.nullIfEmpty ?: it.name

  private fun getDefaultValue(
    annotation: DocProperty?,
    obj: Any,
    property: KProperty<*>,
  ): String {
    if (!annotation?.defaultValue.isNullOrEmpty()) {
      return annotation.defaultValue
    }
    return property.getter.call(obj)?.toString() ?: ""
  }

  private val String.nullIfEmpty: String? get() = this.ifEmpty { null }

  private fun getPropertyTree(docProperty: DocProperty): DocItem {
    if (docProperty.children.isNotEmpty()) {
      return Group(
        name = docProperty.name,
        displayName = docProperty.displayName,
        description = docProperty.description,
        children = docProperty.children.map { getPropertyTree(it) },
        prefix = docProperty.prefix.nullIfEmpty,
        isList = docProperty.isList,
      )
    }
    return Property(
      name = docProperty.name,
      displayName = docProperty.displayName.nullIfEmpty,
      description = docProperty.description.nullIfEmpty,
      defaultValue = docProperty.defaultValue.nullIfEmpty,
      defaultExplanation = docProperty.defaultExplanation.nullIfEmpty,
      removedIn = docProperty.removedIn.nullIfEmpty,
      removalReason = docProperty.removalReason.nullIfEmpty,
    )
  }
}
