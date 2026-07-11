/**
 * Copyright (C) 2025 Tolgee s.r.o. and contributors
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

package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

class ReadOnlyModeInterceptorTest {
  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val authentication = Mockito.mock(TolgeeAuthentication::class.java)

  private val interceptor = ReadOnlyModeInterceptor(authenticationFacade)

  private val mockMvc =
    MockMvcBuilders
      .standaloneSetup(TestController::class.java)
      .addInterceptors(interceptor)
      .build()

  @BeforeEach
  fun setupMocks() {
    whenever(authenticationFacade.authenticatedUser).thenReturn(Mockito.mock(UserAccountDto::class.java))
    whenever(authenticationFacade.authentication).thenReturn(authentication)
    whenever(authenticationFacade.isAuthenticated).thenReturn(true)
    whenever(authenticationFacade.isReadOnly).thenCallRealMethod()
    whenever(authentication.isReadOnly).thenReturn(true)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(authenticationFacade, authentication)
  }

  @Test
  fun `it allows unauthenticated requests`() {
    Mockito.reset(authenticationFacade)
    whenever(authenticationFacade.authentication).thenReturn(null)
    whenever(authenticationFacade.isAuthenticated).thenReturn(false)
    whenever(authenticationFacade.isReadOnly).thenThrow(IllegalStateException("Should not be called"))

    mockMvc.perform(get("/test/read")).andIsOk
    mockMvc.perform(head("/test/read")).andIsOk
    mockMvc.perform(post("/test/write")).andIsOk
    mockMvc.perform(put("/test/write")).andIsOk
    mockMvc.perform(patch("/test/write")).andIsOk
    mockMvc.perform(delete("/test/write")).andIsOk
  }

  @Test
  fun `it allows GET`() {
    mockMvc.perform(get("/test/read")).andIsOk
  }

  @Test
  fun `it allows HEAD`() {
    mockMvc.perform(head("/test/read")).andIsOk
  }

  @Test
  fun `it denies POST`() {
    mockMvc.perform(post("/test/write")).andIsForbidden
  }

  @Test
  fun `it denies PUT`() {
    mockMvc.perform(put("/test/write")).andIsForbidden
  }

  @Test
  fun `it denies PATCH`() {
    mockMvc.perform(patch("/test/write")).andIsForbidden
  }

  @Test
  fun `it denies DELETE`() {
    mockMvc.perform(delete("/test/write")).andIsForbidden
  }

  @Test
  fun `it allows read-only POST when method annotated with AllowInReadOnlyMode`() {
    mockMvc.perform(post("/test/write-allowed")).andIsOk
  }

  @Test
  fun `it allows read-only PUT when method annotated with AllowInReadOnlyMode`() {
    mockMvc.perform(put("/test/write-allowed")).andIsOk
  }

  @Test
  fun `it allows read-only PATCH when method annotated with AllowInReadOnlyMode`() {
    mockMvc.perform(patch("/test/write-allowed")).andIsOk
  }

  @Test
  fun `it allows read-only DELETE when method annotated with AllowInReadOnlyMode`() {
    mockMvc.perform(delete("/test/write-allowed")).andIsOk
  }

  @Test
  fun `it denies GET annotated with WriteOperation`() {
    mockMvc.perform(get("/test/read-requires-rw")).andIsForbidden
  }

  @Test
  fun `it denies HEAD annotated with WriteOperation`() {
    mockMvc.perform(head("/test/read-requires-rw")).andIsForbidden
  }

  @Test
  fun `it allows POST when we are not in read only mode`() {
    whenever(authentication.isReadOnly).thenReturn(false)
    mockMvc.perform(post("/test/write")).andIsOk
  }

  @Test
  fun `it allows PUT when we are not in read only mode`() {
    whenever(authentication.isReadOnly).thenReturn(false)
    mockMvc.perform(put("/test/write")).andIsOk
  }

  @Test
  fun `it allows PATCH when we are not in read only mode`() {
    whenever(authentication.isReadOnly).thenReturn(false)
    mockMvc.perform(patch("/test/write")).andIsOk
  }

  @Test
  fun `it allows DELETE when we are not in read only mode`() {
    whenever(authentication.isReadOnly).thenReturn(false)
    mockMvc.perform(delete("/test/write")).andIsOk
  }

  @RestController
  class TestController {
    @GetMapping("/test/read")
    fun read() = "ok"

    @PostMapping("/test/write")
    fun write() = "ok"

    @PutMapping("/test/write")
    fun put() = "ok"

    @PatchMapping("/test/write")
    fun patch() = "ok"

    @DeleteMapping("/test/write")
    fun delete() = "ok"

    @ReadOnlyOperation
    @PostMapping("/test/write-allowed")
    fun writeAllowed() = "ok"

    @ReadOnlyOperation
    @PutMapping("/test/write-allowed")
    fun putAllowed() = "ok"

    @ReadOnlyOperation
    @PatchMapping("/test/write-allowed")
    fun patchAllowed() = "ok"

    @ReadOnlyOperation
    @DeleteMapping("/test/write-allowed")
    fun deleteAllowed() = "ok"

    @WriteOperation
    @GetMapping("/test/read-requires-rw")
    fun readRequiresRw() = "ok"
  }
}
