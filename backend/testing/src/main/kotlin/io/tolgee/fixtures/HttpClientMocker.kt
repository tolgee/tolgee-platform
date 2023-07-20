package io.tolgee.fixtures

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class HttpClientMocker(private val restTemplate: RestTemplate) {
  data class VerifyTools(
    val captor: KArgumentCaptor<HttpEntity<*>> = argumentCaptor()
  )

  data class Definition(
    var url: (String) -> Boolean = { true },
    var method: (HttpMethod) -> Boolean = { true },
  )

  private lateinit var definition: Definition
  private var answer: (() -> ResponseEntity<String>)? = null
  private var toThrow: Throwable? = null

  private val verifyTools: VerifyTools = VerifyTools()

  private fun updateWhenever() {
    whenever(
      restTemplate.exchange(
        argThat<String> { definition.url(this) },
        argThat { definition.method(this) },
        verifyTools.captor.capture(),
        eq(String::class.java)
      )
    ).apply {
      if (toThrow != null) {
        thenThrow(toThrow!!)
      }

      if (answer != null) {
        thenAnswer { answer!!() }
      }
    }
  }

  fun whenReq(fn: Definition.() -> Unit) {
    definition = Definition().apply(fn)
  }

  fun thenAnswer(response: () -> Any) {
    answer = {
      ResponseEntity(jacksonObjectMapper().writeValueAsString(response()), HttpStatus.OK)
    }
    updateWhenever()
  }

  fun thenThrow(throwable: Throwable) {
    toThrow = throwable
    updateWhenever()
  }

  fun verify(fn: VerifyTools.() -> Unit) {
    verifyTools.fn()
  }
}
