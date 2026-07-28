package io.tolgee.unit.cache

import io.tolgee.component.cache.CacheValueFingerprint
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

class CacheValueFingerprintTest {
  private val fingerprint = CacheValueFingerprint()

  private data class Money(
    val amount: BigDecimal,
    val currency: String,
  )

  private data class WithMoney(
    val money: Money,
    val note: String?,
  )

  private data class FieldAdded(
    val amount: BigDecimal,
    val currency: String,
    val refundable: Boolean,
  )

  private enum class Color { RED, GREEN }

  private enum class ColorPlusOne { RED, GREEN, BLUE }

  private data class WithColor(
    val color: Color,
  )

  @Test
  fun `pins the canonical signature - name, sorted props, project expansion, jdk leaves`() {
    fingerprint
      .signature(Money::class.starProjectedType)
      .assert
      .isEqualTo(
        "io.tolgee.unit.cache.CacheValueFingerprintTest.Money(amount:java.math.BigDecimal,currency:kotlin.String)",
      )
  }

  @Test
  fun `is deterministic`() {
    fingerprint.compute(Money::class).assert.isEqualTo(fingerprint.compute(Money::class))
  }

  private data class WithComputed(
    val a: Int,
  ) {
    val label get() = "x$a"
  }

  @Test
  fun `ignores computed properties without a backing field`() {
    fingerprint
      .signature(WithComputed::class.starProjectedType)
      .assert
      .isEqualTo("io.tolgee.unit.cache.CacheValueFingerprintTest.WithComputed(a:kotlin.Int)")
  }

  @Test
  fun `aggregation of multiple types is order-independent`() {
    fingerprint
      .compute(listOf(typeOf<Money>(), typeOf<FieldAdded>()))
      .assert
      .isEqualTo(fingerprint.compute(listOf(typeOf<FieldAdded>(), typeOf<Money>())))
  }

  @Test
  fun `produces a 12 char hex fingerprint`() {
    fingerprint.compute(Money::class).assert.matches({ it.matches(Regex("[0-9a-f]{12}")) }, "12 hex chars")
  }

  @Test
  fun `changes when a property is added`() {
    fingerprint.compute(Money::class).assert.isNotEqualTo(fingerprint.compute(FieldAdded::class))
  }

  @Test
  fun `recurses into nested project types`() {
    fingerprint
      .signature(WithMoney::class.starProjectedType)
      .assert
      .contains("money:io.tolgee.unit.cache.CacheValueFingerprintTest.Money(amount:")
  }

  @Test
  fun `expands enum constants`() {
    fingerprint
      .signature(WithColor::class.starProjectedType)
      .assert
      .contains("Color{GREEN,RED}")
  }

  @Test
  fun `changes when an enum gains a constant`() {
    val two = Color::class.starProjectedType
    val three = ColorPlusOne::class.starProjectedType
    fingerprint.signature(two).assert.contains("{GREEN,RED}")
    fingerprint.signature(three).assert.contains("{BLUE,GREEN,RED}")
  }

  @Test
  fun `distinguishes generic type arguments in isolation`() {
    val listOfInt = List::class.createType(listOf(KTypeProjection.invariant(Int::class.starProjectedType)))
    val listOfString = List::class.createType(listOf(KTypeProjection.invariant(String::class.starProjectedType)))
    fingerprint.compute(listOfInt).assert.isNotEqualTo(fingerprint.compute(listOfString))
  }

  @Test
  fun `distinguishes collection kind in isolation`() {
    val listOfInt = List::class.createType(listOf(KTypeProjection.invariant(Int::class.starProjectedType)))
    val setOfInt = Set::class.createType(listOf(KTypeProjection.invariant(Int::class.starProjectedType)))
    fingerprint.compute(listOfInt).assert.isNotEqualTo(fingerprint.compute(setOfInt))
  }

  private data class Node(
    val value: Int,
    val next: Node?,
  )

  @Test
  fun `handles self-referential types without infinite recursion`() {
    fingerprint
      .signature(Node::class.starProjectedType)
      .assert
      .contains("@io.tolgee.unit.cache.CacheValueFingerprintTest.Node")
    fingerprint.compute(Node::class).assert.matches({ it.matches(Regex("[0-9a-f]{12}")) }, "12 hex chars")
  }

  @Test
  fun `distinguishes nullability in isolation`() {
    val notNull = String::class.createType(nullable = false)
    val nullable = String::class.createType(nullable = true)
    fingerprint.compute(notNull).assert.isNotEqualTo(fingerprint.compute(nullable))
  }
}
