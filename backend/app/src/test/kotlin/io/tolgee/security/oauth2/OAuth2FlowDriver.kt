/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.oauth2

import io.tolgee.testing.assert
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Drives the authorization-code flow over HTTP the way the browser does — `/oauth2/authorize`, then the consent
 * screen's JWT-authenticated `/v2/oauth2` calls, then the token exchange. Shared so the protocol tests and the
 * Tolgee-specific flow tests exercise one driver rather than two copies that can drift apart.
 */
class OAuth2FlowDriver(
  private val mvc: MockMvc,
) {
  data class PendingConsent(
    val jwt: String,
    val state: String,
    val verifier: String,
    val clientId: String,
    val redirect: String,
  )

  /** The `/oauth2/authorize` redirect a client would follow; it carries no credentials. */
  fun authorize(
    clientId: String,
    redirect: String,
    params: Map<String, String?> = emptyMap(),
  ): ResultActions {
    val builder =
      UriComponentsBuilder
        .fromPath("/oauth2/authorize")
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirect)
    params.forEach { (name, value) -> value?.let { builder.queryParam(name, it) } }
    return mvc.perform(get(builder.build().toUriString()))
  }

  fun startAuthorization(
    jwt: String,
    clientId: String,
    redirect: String,
    params: Map<String, String?>,
  ): ResultActions {
    val body = mutableMapOf<String, Any>("client_id" to clientId, "redirect_uri" to redirect)
    params.forEach { (name, value) -> value?.let { body[name] = it } }
    return mvc.perform(
      post("/v2/oauth2/authorize")
        .header("Authorization", "Bearer $jwt")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body)),
    )
  }

  fun startPendingConsent(
    jwt: String,
    clientId: String,
    redirect: String,
    scope: String = "translations.view",
    clientState: String? = "client-state",
    hintProjectId: Long? = null,
    verifier: String = randomVerifier(),
  ): PendingConsent {
    val response =
      startAuthorization(
        jwt,
        clientId,
        redirect,
        mapOf(
          "response_type" to "code",
          "scope" to scope,
          "state" to clientState,
          "code_challenge" to s256Challenge(verifier),
          "code_challenge_method" to "S256",
          "project" to hintProjectId?.toString(),
        ),
      ).andReturn().response.contentAsString
    val state = mapper.readTree(response).get("consentState")?.asString()
    state.assert.withFailMessage("no pending consent was opened: $response").isNotNull()
    return PendingConsent(jwt, state!!, verifier, clientId, redirect)
  }

  fun consentInfo(
    jwt: String,
    state: String,
  ): ResultActions =
    mvc.perform(get("/v2/oauth2/consent-info").header("Authorization", "Bearer $jwt").param("state", state))

  fun submitConsent(
    pending: PendingConsent,
    approvedScopes: List<String> = listOf("translations.view"),
    projectId: Long? = null,
  ): ResultActions {
    val body =
      mutableMapOf<String, Any>(
        "state" to pending.state,
        "scopes" to approvedScopes,
        "projectScope" to if (projectId == null) "ALL_PROJECTS" else "SINGLE_PROJECT",
      )
    projectId?.let { body["projectId"] = it }
    return mvc.perform(
      post("/v2/oauth2/consent")
        .header("Authorization", "Bearer ${pending.jwt}")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body)),
    )
  }

  fun submitConsentBody(
    pending: PendingConsent,
    json: String,
  ): MvcResult =
    mvc
      .perform(
        post("/v2/oauth2/consent")
          .header("Authorization", "Bearer ${pending.jwt}")
          .contentType(MediaType.APPLICATION_JSON)
          .content(json),
      ).andReturn()

  fun consentRedirect(
    pending: PendingConsent,
    approvedScopes: List<String> = listOf("translations.view"),
    projectId: Long? = null,
  ): String {
    val body = submitConsent(pending, approvedScopes, projectId).andReturn().response.contentAsString
    val url = mapper.readTree(body).get("redirectUrl")?.asString()
    url.assert.withFailMessage("consent returned no redirect: $body").isNotNull()
    return url!!
  }

  fun exchangeCode(
    code: String,
    clientId: String,
    redirect: String,
    verifier: String,
  ): ResultActions =
    mvc.perform(
      post("/oauth2/token")
        .param("grant_type", "authorization_code")
        .param("code", code)
        .param("redirect_uri", redirect)
        .param("client_id", clientId)
        .param("code_verifier", verifier)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED),
    )

  fun refresh(
    refreshToken: String,
    clientId: String,
    scope: String? = null,
  ): ResultActions {
    val request =
      post("/oauth2/token")
        .param("grant_type", "refresh_token")
        .param("refresh_token", refreshToken)
        .param("client_id", clientId)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
    scope?.let { request.param("scope", it) }
    return mvc.perform(request)
  }

  fun completeFlow(
    jwt: String,
    clientId: String,
    redirect: String,
    scope: String = "translations.view",
    approvedScopes: List<String> = listOf("translations.view"),
    projectId: Long? = null,
    hintProjectId: Long? = null,
  ): JsonNode {
    val pending =
      startPendingConsent(jwt, clientId, redirect, scope = scope, hintProjectId = hintProjectId)
    val redirectUrl = consentRedirect(pending, approvedScopes, projectId)
    val code = queryParam(redirectUrl, "code")
    code.assert.withFailMessage("consent did not deliver a code: $redirectUrl").isNotNull()
    val body =
      exchangeCode(code!!, clientId, redirect, pending.verifier).andReturn().response.contentAsString
    val tree = mapper.readTree(body)
    tree
      .get("access_token")
      .assert
      .withFailMessage(body)
      .isNotNull()
    return tree
  }

  fun accessToken(
    jwt: String,
    clientId: String,
    redirect: String,
    scope: String = "translations.view",
    approvedScopes: List<String> = listOf("translations.view"),
    projectId: Long? = null,
  ): String = completeFlow(jwt, clientId, redirect, scope, approvedScopes, projectId).get("access_token").asString()

  fun queryParam(
    url: String,
    name: String,
  ): String? =
    UriComponentsBuilder
      .fromUriString(url)
      .build()
      .queryParams
      .getFirst(name)

  companion object {
    private val mapper = jacksonObjectMapper()

    fun randomVerifier(): String {
      val bytes = ByteArray(32)
      SecureRandom().nextBytes(bytes)
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun s256Challenge(verifier: String): String {
      val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
  }
}
