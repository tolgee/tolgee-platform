package io.tolgee.ee.guards

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.tolgee.AbstractSpringTest
import io.tolgee.activity.data.RevisionType
import io.tolgee.batch.state.ExecutionState
import io.tolgee.component.ThirdPartyAuthTypeConverter
import io.tolgee.component.bucket.TokenBucket
import io.tolgee.constants.MtServiceType
import io.tolgee.ee.model.EeSubscription
import io.tolgee.formats.ExportFormat
import io.tolgee.model.DismissedAnnouncementId
import io.tolgee.model.automations.AutomationActionType
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MapKeyEnumerated
import jakarta.persistence.metamodel.Attribute
import jakarta.persistence.metamodel.ManagedType
import jakarta.persistence.metamodel.MapAttribute
import jakarta.persistence.metamodel.PluralAttribute
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import java.lang.reflect.Type as ReflectType

/**
 * Lives in `:ee-test` so the metamodel sees every module's entities: `:server-app` pulls `:ee-app` in only
 * conditionally, so from there the EE entities would vanish from the scan while the floors below still passed.
 */
@SpringBootTest
class OrdinalBoundEnumGuardTest : AbstractSpringTest() {
  // Redisson structures on the default codec are not auto-discovered like JPA columns: any such structure whose
  // value transitively holds an enum must be listed here, or that enum serializes by ordinal with no guard.
  private val nonSpringCacheOrdinalCodecRedisTypes = listOf(ExecutionState::class.java, TokenBucket::class.java)

  private val pinnedOrders =
    mapOf<Class<*>, List<String>>(
      OrganizationRoleType::class.java to listOf("MEMBER", "OWNER", "MAINTAINER"),
      RevisionType::class.java to listOf("ADD", "MOD", "DEL"),
      TranslationState::class.java to listOf("UNTRANSLATED", "TRANSLATED", "REVIEWED", "DISABLED"),
      MtServiceType::class.java to listOf("GOOGLE", "AWS", "DEEPL", "AZURE", "BAIDU", "PROMPT"),
      TranslationCommentState::class.java to listOf("RESOLUTION_NOT_NEEDED", "NEEDS_RESOLUTION", "RESOLVED"),
      AutomationActionType::class.java to listOf("CONTENT_DELIVERY_PUBLISH", "WEBHOOK", "SLACK_SUBSCRIPTION"),
      ExportFormat::class.java to
        listOf(
          "JSON",
          "JSON_TOLGEE",
          "XLIFF",
          "PO",
          "APPLE_STRINGS_STRINGSDICT",
          "APPLE_XLIFF",
          "ANDROID_XML",
          "COMPOSE_XML",
          "FLUTTER_ARB",
          "PROPERTIES",
          "YAML_RUBY",
          "YAML",
          "JSON_I18NEXT",
          "CSV",
          "RESX_ICU",
          "XLSX",
          "APPLE_XCSTRINGS",
          "ANDROID_SDK",
          "APPLE_SDK",
        ),
      FileIssueParamType::class.java to
        listOf(
          "KEY_NAME",
          "KEY_ID",
          "LANGUAGE_ID",
          "KEY_INDEX",
          "VALUE",
          "LINE",
          "FILE_NODE_ORIGINAL",
          "LANGUAGE_NAME",
        ),
      FileIssueType::class.java to
        listOf(
          "KEY_IS_NOT_STRING",
          "MULTIPLE_VALUES_FOR_KEY_AND_LANGUAGE",
          "VALUE_IS_NOT_STRING",
          "KEY_IS_EMPTY",
          "VALUE_IS_EMPTY",
          "PO_MSGCTXT_NOT_SUPPORTED",
          "ID_ATTRIBUTE_NOT_PROVIDED",
          "TARGET_NOT_PROVIDED",
          "TRANSLATION_TOO_LONG",
          "KEY_IS_BLANK",
          "TRANSLATION_DEFINED_IN_ANOTHER_FILE",
          "INVALID_CUSTOM_VALUES",
          "DESCRIPTION_TOO_LONG",
          "TRANSLATION_EXCEEDS_CHAR_LIMIT",
        ),
      BatchJobChunkExecutionStatus::class.java to listOf("PENDING", "RUNNING", "SUCCESS", "FAILED", "CANCELLED"),
    )

  @Test
  fun `pinned enums keep their existing constants at their existing ordinals`() {
    pinnedOrders.forEach { (enumType, pinned) ->
      val actual = enumType.enumConstants.map { (it as Enum<*>).name }
      assertThat(actual)
        .withFailMessage(
          "${enumType.simpleName}'s constant order is part of the schema. Existing constants must keep their " +
            "ordinals; a new one must be appended AND added to the end of the pinned list here.\n" +
            "  pinned: $pinned\n  actual: $actual",
        ).containsExactlyElementsOf(pinned)
    }
  }

  @Test
  fun `every enum a JPA column binds by ordinal is pinned above`() {
    val unpinned = ordinalBoundJpaEnums() - pinnedOrders.keys
    assertThat(unpinned)
      .withFailMessage(
        "These enums are persisted by ordinal, so their constant order is part of the schema. Pin each above " +
          "(or map the column with @Enumerated(EnumType.STRING)):\n" +
          bulletList(unpinned),
      ).isEmpty()
  }

  @Test
  fun `entity scan is non-trivial (a broken or empty metamodel cannot masquerade as success)`() {
    assertThat(managedTypesOwningTheirColumns()).hasSizeGreaterThan(50)
    assertThat(managedTypesOwningTheirColumns().map { it.javaType })
      .describedAs("EE entities are in the metamodel — the reason this guard lives in :ee-test")
      .contains(EeSubscription::class.java)
    assertThat(enumAttributes()).hasSizeGreaterThan(10)
    assertThat(ordinalBoundJpaEnums()).isNotEmpty()
    assertThat(enumAttributes().filter { it is PluralAttribute<*, *, *> })
      .describedAs("@ElementCollection enum columns resolve")
      .isNotEmpty()
    assertThat(enumAttributes().filter { (it.javaMember as? Field)?.type?.isArray == true })
      .describedAs("enum-array columns resolve")
      .isNotEmpty()
    assertThat(enumAttributes().filter { it.javaMember !is Field })
      .describedAs(
        "enum attributes are field-access — enumTypeOf reads their real type off the Field, not the metamodel",
      ).isEmpty()
  }

  @Test
  fun `every enum the default-codec Redis structures serialize is pinned above`() {
    val reachable = nonSpringCacheOrdinalCodecRedisTypes.flatMap { enumFieldsOf(it) }
    val unpinned = reachable.toSet() - pinnedOrders.keys
    assertThat(unpinned)
      .withFailMessage(
        "These enums are serialized by ordinal into Redis structures that keep the default codec, where entries " +
          "have no TTL and cannot be recomputed. Pin each above:\n" +
          bulletList(unpinned),
      ).isEmpty()
    assertThat(reachable)
      .describedAs("enum fields reachable from the default-codec Redis types")
      .isNotEmpty()
  }

  @Test
  fun `finds each way a serialized object can hold an enum, including through a nested object`() {
    assertThat(enumFieldsOf(RedisFixture::class.java))
      .describedAs("a Kotlin List<out E>/Map<K, out V> field arrives as a wildcard, not a class")
      .contains(
        Scope::class.java,
        TranslationState::class.java,
        RevisionType::class.java,
        OrganizationRoleType::class.java,
        MtServiceType::class.java,
        TranslationCommentState::class.java,
        AutomationActionType::class.java,
      )
    assertThat(enumFieldsOf(RevisionType::class.java))
      .describedAs("a Redis structure whose value type is itself an enum")
      .containsExactly(RevisionType::class.java)
  }

  @Test
  fun `scans a real Embeddable but not the IdClass mirrors Hibernate also reports as embeddables`() {
    assertThat(ownsItsColumns(EmbeddableFixture::class.java)).isTrue()
    assertThat(ownsItsColumns(DismissedAnnouncementId::class.java)).isFalse()
    assertThat(entityManager.metamodel.embeddables.map { it.javaType })
      .describedAs("an @IdClass mirror, whose fields copy the entity's @Ids without their mapping annotations")
      .contains(DismissedAnnouncementId::class.java)
    assertThat(managedTypesOwningTheirColumns().map { it.javaType })
      .doesNotContain(DismissedAnnouncementId::class.java)
  }

  @Test
  fun `classifies each way a column can bind an enum`() {
    assertThat(isOrdinalBound(field("bare"))).describedAs("bare").isTrue()
    assertThat(isOrdinalBound(field("ordinalArray"))).describedAs("ordinalArray").isTrue()
    assertThat(isOrdinalBound(field("convertedToOrdinal"))).describedAs("convertedToOrdinal").isTrue()
    assertThat(isOrdinalBound(field("stringBound"))).describedAs("stringBound").isFalse()
    assertThat(isOrdinalBound(field("nameArray"))).describedAs("nameArray").isFalse()
    assertThat(isOrdinalBound(field("convertedToName"))).describedAs("convertedToName").isFalse()
    assertThat(isOrdinalBoundMapKey(field("ordinalMapKey"))).describedAs("ordinalMapKey").isTrue()
    assertThat(isOrdinalBoundMapKey(field("nameMapKey"))).describedAs("nameMapKey").isFalse()
  }

  private fun field(name: String) = Fixture::class.java.getDeclaredField(name)

  private fun enumFieldsOf(
    type: Class<*>,
    seen: MutableSet<Class<*>> = mutableSetOf(),
  ): List<Class<*>> {
    if (!seen.add(type)) return emptyList()
    if (type.isEnum) return listOf(type)
    return generateSequence(type) { it.superclass }
      .takeWhile { it != Any::class.java }
      .flatMap { it.declaredFields.asSequence() }
      .filterNot { Modifier.isStatic(it.modifiers) }
      .flatMap { heldTypesOf(it.genericType) }
      .flatMap { held ->
        when {
          held.isEnum -> listOf(held)
          held.name.startsWith("java.") -> emptyList()
          else -> enumFieldsOf(held, seen)
        }
      }.toList()
  }

  private fun heldTypesOf(type: ReflectType): List<Class<*>> =
    when (type) {
      is ParameterizedType ->
        listOfNotNull(type.rawType as? Class<*>) + type.actualTypeArguments.flatMap { heldTypesOf(it) }
      is WildcardType -> type.upperBounds.flatMap { heldTypesOf(it) }
      is GenericArrayType -> heldTypesOf(type.genericComponentType)
      is Class<*> -> heldTypesOfClass(type)
      else -> emptyList()
    }

  private fun ordinalBoundJpaEnums(): Set<Class<*>> =
    managedTypesOwningTheirColumns()
      .flatMap { it.attributes }
      .flatMap { ordinalBoundEnumsOf(it) }
      .toSet()

  private fun ordinalBoundEnumsOf(attribute: Attribute<*, *>): List<Class<*>> {
    val member = attribute.javaMember as? AnnotatedElement ?: return emptyList()
    return listOfNotNull(
      enumTypeOf(attribute)?.takeIf { isOrdinalBound(member) },
      mapKeyEnumTypeOf(attribute)?.takeIf { isOrdinalBoundMapKey(member) },
    )
  }

  private fun mapKeyEnumTypeOf(attribute: Attribute<*, *>): Class<*>? =
    (attribute as? MapAttribute<*, *, *>)?.keyType?.javaType?.takeIf { it.isEnum }

  private fun enumAttributes(): List<Attribute<*, *>> =
    managedTypesOwningTheirColumns()
      .flatMap { it.attributes }
      .filter { enumTypeOf(it) != null }

  private fun managedTypesOwningTheirColumns(): Collection<ManagedType<*>> =
    entityManager.metamodel.entities +
      entityManager.metamodel.embeddables.filter { ownsItsColumns(it.javaType) }

  private fun ownsItsColumns(type: Class<*>) = type.isAnnotationPresent(Embeddable::class.java)

  private fun heldTypesOfClass(type: Class<*>): List<Class<*>> {
    if (type.isArray) return heldTypesOf(type.componentType)
    return listOf(type)
  }

  private fun enumTypeOf(attribute: Attribute<*, *>): Class<*>? {
    val declared =
      when (attribute) {
        is PluralAttribute<*, *, *> -> attribute.elementType.javaType
        // Hibernate erases an enum-array attribute's metamodel javaType to Enum[]; the field keeps the real type.
        else -> (attribute.javaMember as? Field)?.type ?: attribute.javaType
      }
    return elementTypeOf(declared).takeIf { it.isEnum }
  }

  private fun bulletList(types: Collection<Class<*>>) =
    types.map { it.name }.sorted().joinToString("\n") { "  - $it" }

  private fun elementTypeOf(declared: Class<*>): Class<*> {
    if (declared.isArray) return declared.componentType
    return declared
  }

  /** A JPA map key with no `@MapKeyEnumerated` defaults to ORDINAL. */
  private fun isOrdinalBoundMapKey(member: AnnotatedElement): Boolean =
    member.getAnnotation(MapKeyEnumerated::class.java)?.value != EnumType.STRING

  /** A JPA enum column with no `@Enumerated` defaults to ORDINAL. */
  private fun isOrdinalBound(member: AnnotatedElement): Boolean {
    member.getAnnotation(Convert::class.java)?.let { if (!it.disableConversion) return convertsToNumber(it.converter) }
    val type = member.getAnnotation(Type::class.java)
    if (type?.value == EnumArrayType::class) {
      return type.parameters.none { it.name == EnumArrayType.SQL_ARRAY_TYPE && it.value == "varchar" }
    }
    return member.getAnnotation(Enumerated::class.java)?.value != EnumType.STRING
  }

  private fun convertsToNumber(converter: KClass<*>): Boolean {
    val target =
      converter.java.genericInterfaces
        .filterIsInstance<ParameterizedType>()
        .firstOrNull { it.rawType == AttributeConverter::class.java }
        ?.actualTypeArguments
        ?.getOrNull(1) as? Class<*> ?: return true
    return Number::class.java.isAssignableFrom(target)
  }

  @Suppress("unused")
  private class Fixture {
    var bare: Scope? = null

    @Enumerated(EnumType.STRING)
    var stringBound: Scope? = null

    @Type(EnumArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
    var nameArray: Array<Scope>? = null

    @Type(EnumArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "int")])
    var ordinalArray: Array<Scope>? = null

    @Convert(converter = ThirdPartyAuthTypeConverter::class)
    var convertedToName: ThirdPartyAuthType? = null

    @Convert(converter = ScopeOrdinalConverter::class)
    var convertedToOrdinal: Scope? = null

    var ordinalMapKey: MutableMap<Scope, String> = mutableMapOf()

    @MapKeyEnumerated(EnumType.STRING)
    var nameMapKey: MutableMap<Scope, String> = mutableMapOf()
  }

  @Embeddable
  private class EmbeddableFixture

  @Suppress("unused")
  private open class RedisFixtureBase {
    var inherited: RevisionType? = null
  }

  @Suppress("unused")
  private class RedisFixtureNested {
    var deep: TranslationCommentState? = null
  }

  /** Distinct from [RedisFixtureNested]: sharing a type would let `seen` mask one of the two paths into it. */
  @Suppress("unused")
  private class RedisFixtureNestedElement {
    var deep: AutomationActionType? = null
  }

  @Suppress("unused")
  private class RedisFixture : RedisFixtureBase() {
    var direct: Scope? = null
    var inArray: Array<TranslationState>? = null
    var inList: List<OrganizationRoleType> = emptyList()
    var inMap: Map<String, MtServiceType> = emptyMap()
    var nested: RedisFixtureNested? = null
    var nestedInList: List<RedisFixtureNestedElement> = emptyList()
    var self: RedisFixture? = null
  }

  private class ScopeOrdinalConverter : AttributeConverter<Scope, Int> {
    override fun convertToDatabaseColumn(attribute: Scope?) = attribute?.ordinal

    override fun convertToEntityAttribute(dbData: Int?) = dbData?.let { Scope.entries[it] }
  }
}
