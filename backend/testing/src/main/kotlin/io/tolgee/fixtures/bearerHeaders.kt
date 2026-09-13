package io.tolgee.fixtures

import org.springframework.http.HttpHeaders

fun bearerHeaders(token: String): HttpHeaders =
  HttpHeaders().apply { add(HttpHeaders.AUTHORIZATION, AuthorizedRequestFactory.getBearerTokenString(token)) }
