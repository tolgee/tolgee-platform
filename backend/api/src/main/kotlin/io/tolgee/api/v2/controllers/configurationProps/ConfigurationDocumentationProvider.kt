package io.tolgee.api.v2.controllers.configurationProps

import io.tolgee.configuration.annotations.AdditionalDocsProperties
import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

class ConfigurationDocumentationProvider {
  private val globalItems: MutableList<DocItem> = mutableListOf()
  val docs by lazy {
    globalItems + listOf(handleObject(TolgeeProperties(), null))
  }

  private fun handleObject(
    obj: Any,
    parent: KProperty<*>?,
  ): Group {
    val additionalProps: MutableList<DocItem> = mutableListOf()
    obj::class.findAnnotations(AdditionalDocsProperties::class).forEach { additionalProp ->
      if (additionalProp.global) {
        additionalProp.properties.forEach {
          globalItems.add(getPropertyTree(it))
        }
        return@forEach
      }
      additionalProp.properties.forEach {
        additionalProps.add(getPropertyTree(it))
      }
    }

    val objDef = obj::class.findAnnotations(DocProperty::class).singleOrNull()
    val confPropsDef = obj::class.findAnnotations(ConfigurationProperties::class).singleOrNull()
    val props =
      obj::class.declaredMemberProperties.mapNotNull {
        handleProperty(it, obj)
      }.sortedWith(
        compareBy(
          { it is Group },
          { it.name },
        ),
      )

    val name =
      objDef?.name?.nullIfEmpty ?: parent?.name ?: confPropsDef?.prefix?.replace(
        "(.*)\\.(.+?)\$".toRegex(),
        "$1",
      )?.nullIfEmpty
        ?: throw RuntimeException("No name for $obj with parent $parent")
    return Group(
      name = name,
      displayName = objDef?.displayName?.nullIfEmpty,
      description = objDef?.description?.nullIfEmpty,
      children = props + additionalProps,
      prefix =
        objDef?.prefix?.nullIfEmpty ?: confPropsDef?.prefix?.nullIfEmpty
          ?: throw NullPointerException("No prefix for ${obj::class.simpleName}"),
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

    return when {
      it.returnType.isSubtypeOfOneOf(
        typeOf<String?>(),
        typeOf<Int?>(),
        typeOf<Long?>(),
        typeOf<Double?>(),
        typeOf<List<*>?>(),
        typeOf<Boolean?>(),
      ) || (it.returnType.javaType as? Class<*>)?.isEnum == true -> {
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
        val child =
          it.getter.call(obj)
            ?: throw RuntimeException("Property ${it.name} is null")
        handleObject(child, it)
      }
    }
  }

  private fun KType.isSubtypeOfOneOf(vararg types: KType): Boolean {
    types.forEach {
      if (this.isSubtypeOf(it)) {
        return true
      }
    }
    return false
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
      return annotation?.defaultValue ?: ""
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
