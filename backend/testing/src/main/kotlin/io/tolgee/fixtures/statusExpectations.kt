package io.tolgee.fixtures

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.constants.Message
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.MvcResultAssert
import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.BigDecimalAssert
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

val ResultActions.andIsOk: ResultActions
  get() = this.tryPrettyPrinting { this.andExpect(status().isOk) }

val ResultActions.andIsNoContent: ResultActions
  get() = this.tryPrettyPrinting { this.andExpect(status().isNoContent) }

val ResultActions.andIsUnauthorized: ResultActions
  get() = this.tryPrettyPrinting { this.andExpect(status().isUnauthorized) }

val ResultActions.andIsNotFound: ResultActions
  get() = this.tryPrettyPrinting { this.andExpect(status().isNotFound) }

val ResultActions.andIsCreated: ResultActions
  get() = this.tryPrettyPrinting { this.andExpect(status().isCreated) }

val ResultActions.andIsBadRequest: ResultActions
  get() = this.tryPrettyPrinting { this.andExpect(status().isBadRequest) }

val ResultActions.andIsNotModified: ResultActions
  get() = this.tryPrettyPrinting { this.andExpect(status().isNotModified) }

val ResultActions.andIsRateLimited: ResultActions
  get() = this.tryPrettyPrinting { this.andExpect(status().isTooManyRequests) }

fun ResultActions.andHasErrorMessage(message: Message): ResultActions {
  return this.tryPrettyPrinting {
    this.andAssertThatJson {
      node("code").isEqualTo(message.code)
    }
  }
}

val ResultActions.andIsForbidden: ResultActions
  get() = this.tryPrettyPrinting { this.andExpect(status().isForbidden) }

val ResultActions.andAssertResponse: MvcResultAssert
  get() = assertThat(this.andReturn())

val ResultActions.andAssertThatJson: JsonAssert.ConfigurableJsonAssert
  get() {
    return assertThatJson(this.andReturn().response.contentAsString)
  }

fun ResultActions.andAssertThatJson(jsonAssert: JsonAssert.ConfigurableJsonAssert.() -> Unit): ResultActions {
  tryPrettyPrinting {
    jsonAssert(assertThatJson(this.andGetContentAsString))
    this
  }
  return this
}

fun ResultActions.tryPrettyPrinting(fn: ResultActions.() -> ResultActions): ResultActions {
  try {
    return fn()
  } catch (e: Error) {
    try {
      this.andPrettyPrint
    } catch (e: Exception) {
      println("Cannot pretty print :(")
    } finally {
      throw e
    }
  }
}

val ResultActions.andGetContentAsString
  get() = this.andReturn().response.getContentAsString(StandardCharsets.UTF_8)

val ResultActions.andAssertError
  get() = assertThat(this.andReturn()).error()

val ResultActions.andPrettyPrint: ResultActions
  get() =
    jacksonObjectMapper().let { mapper ->
      val parsed = mapper.readValue<Any>(this.andGetContentAsString)
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed))
      return this
    }

fun JsonAssert.node(
  node: String,
  rfn: JsonAssert.() -> Unit,
): JsonAssert {
  rfn(node(node))
  return this
}

val JsonAssert.isValidId: BigDecimalAssert
  get() {
    return this.asNumber().isGreaterThan(BigDecimal(10000000))
  }

fun JsonAssert.isPermissionScopes(type: ProjectPermissionType): JsonAssert {
  val expanded = Scope.expand(type.availableScopes).toList()
  this.isArray.containsAll(expanded).hasSize(expanded.size)
  return this
}
