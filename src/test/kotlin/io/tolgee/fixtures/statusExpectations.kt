package io.tolgee.fixtures

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.assertions.MvcResultAssert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

val ResultActions.andIsOk: ResultActions
    get() = this.andExpect(status().isOk)

val ResultActions.andIsBadRequest: ResultActions
    get() = this.andExpect(status().isBadRequest)


val ResultActions.andIsForbidden: ResultActions
    get() = this.andExpect(status().isForbidden)


val ResultActions.andAssertResponse: MvcResultAssert
    get() = assertThat(this.andReturn())

val ResultActions.andAssertThatJson
    get() = assertThatJson(this.andReturn().response.contentAsString)
