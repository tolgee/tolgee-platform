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
import io.tolgee.model.UserAccount
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

class AdminAccessInterceptorTest {
  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val authentication = Mockito.mock(TolgeeAuthentication::class.java)

  private val userAccount = Mockito.mock(UserAccountDto::class.java)

  private val interceptor = AdminAccessInterceptor(authenticationFacade)

  private val mockMvc =
    MockMvcBuilders
      .standaloneSetup(TestController::class.java)
      .addInterceptors(interceptor)
      .build()

  @BeforeEach
  fun setupMocks() {
    whenever(authenticationFacade.authentication).thenReturn(authentication)
    whenever(authenticationFacade.isAuthenticated).thenReturn(true)
    whenever(authenticationFacade.authenticatedUser).thenReturn(userAccount)
    whenever(userAccount.role).thenReturn(UserAccount.Role.USER)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(authenticationFacade, authentication, userAccount)
  }

  @Test
  fun `it allows unauthenticated requests`() {
    whenever(authenticationFacade.authentication).thenReturn(null)
    whenever(authenticationFacade.isAuthenticated).thenReturn(false)

    mockMvc.perform(get("/admin/read")).andIsOk
    mockMvc.perform(post("/admin/write")).andIsOk
    mockMvc.perform(put("/admin/write")).andIsOk
    mockMvc.perform(patch("/admin/write")).andIsOk
    mockMvc.perform(delete("/admin/write")).andIsOk
  }

  @Test
  fun `it allows GET from admin`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.ADMIN)
    mockMvc.perform(get("/admin/read")).andIsOk
  }

  @Test
  fun `it allows HEAD from admin`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.ADMIN)
    mockMvc.perform(head("/admin/read")).andIsOk
  }

  @Test
  fun `it allows POST from admin`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.ADMIN)
    mockMvc.perform(post("/admin/write")).andIsOk
  }

  @Test
  fun `it allows PUT from admin`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.ADMIN)
    mockMvc.perform(put("/admin/write")).andIsOk
  }

  @Test
  fun `it allows PATCH from admin`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.ADMIN)
    mockMvc.perform(patch("/admin/write")).andIsOk
  }

  @Test
  fun `it allows DELETE from admin`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.ADMIN)
    mockMvc.perform(delete("/admin/write")).andIsOk
  }

  @Test
  fun `it allows GET from supporter`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(get("/admin/read")).andIsOk
  }

  @Test
  fun `it allows HEAD from supporter`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(head("/admin/read")).andIsOk
  }

  @Test
  fun `it denies POST from supporter`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(post("/admin/write")).andIsForbidden
  }

  @Test
  fun `it denies PUT from supporter`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(put("/admin/write")).andIsForbidden
  }

  @Test
  fun `it denies PATCH from supporter`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(patch("/admin/write")).andIsForbidden
  }

  @Test
  fun `it denies DELETE from supporter`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(delete("/admin/write")).andIsForbidden
  }

  @Test
  fun `it allows POST from supporter when method annotated with AllowInReadOnlyMode`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(post("/admin/write-allowed")).andIsOk
  }

  @Test
  fun `it allows PUT from supporter when method annotated with AllowInReadOnlyMode`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(put("/admin/write-allowed")).andIsOk
  }

  @Test
  fun `it allows PATCH from supporter when method annotated with AllowInReadOnlyMode`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(patch("/admin/write-allowed")).andIsOk
  }

  @Test
  fun `it allows DELETE from supporter when method annotated with AllowInReadOnlyMode`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(delete("/admin/write-allowed")).andIsOk
  }

  @Test
  fun `it denies GET from supporter when method annotated with WriteOperation`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(get("/admin/read-requires-rw")).andIsForbidden
  }

  @Test
  fun `it denies HEAD from supporter when method annotated with WriteOperation`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    mockMvc.perform(head("/admin/read-requires-rw")).andIsForbidden
  }

  @Test
  fun `it denies GET from user`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.USER)
    mockMvc.perform(get("/admin/read")).andIsForbidden
  }

  @Test
  fun `it denies HEAD from user`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.USER)
    mockMvc.perform(head("/admin/read")).andIsForbidden
  }

  @Test
  fun `it denies POST from user`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.USER)
    mockMvc.perform(post("/admin/write")).andIsForbidden
  }

  @Test
  fun `it denies PUT from user`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.USER)
    mockMvc.perform(put("/admin/write")).andIsForbidden
  }

  @Test
  fun `it denies PATCH from user`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.USER)
    mockMvc.perform(patch("/admin/write")).andIsForbidden
  }

  @Test
  fun `it denies DELETE from user`() {
    whenever(userAccount.role).thenReturn(UserAccount.Role.USER)
    mockMvc.perform(delete("/admin/write")).andIsForbidden
  }

  @RestController
  class TestController {
    @GetMapping("/admin/read")
    fun read() = "ok"

    @PostMapping("/admin/write")
    fun write() = "ok"

    @PutMapping("/admin/write")
    fun put() = "ok"

    @PatchMapping("/admin/write")
    fun patch() = "ok"

    @DeleteMapping("/admin/write")
    fun delete() = "ok"

    @WriteOperation
    @GetMapping("/admin/read-requires-rw")
    fun readRequiresRw() = "ok"

    @ReadOnlyOperation
    @PostMapping("/admin/write-allowed")
    fun writeAllowed() = "ok"

    @ReadOnlyOperation
    @PutMapping("/admin/write-allowed")
    fun putAllowed() = "ok"

    @ReadOnlyOperation
    @PatchMapping("/admin/write-allowed")
    fun patchAllowed() = "ok"

    @ReadOnlyOperation
    @DeleteMapping("/admin/write-allowed")
    fun deleteAllowed() = "ok"
  }
}
