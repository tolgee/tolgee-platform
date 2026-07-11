/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
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

package io.tolgee.unit

import io.tolgee.component.ResilientCacheAccessor
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.redisson.client.RedisException
import org.springframework.cache.Cache

class ResilientCacheAccessorTest {
  private val cache = mock<Cache>()
  private val accessor = ResilientCacheAccessor()

  @BeforeEach
  fun setup() {
    whenever(cache.name).thenReturn("testCache")
  }

  @Test
  fun `normal get returns value`() {
    whenever(cache.get("key1", String::class.java)).thenReturn("value1")

    val result = accessor.get(cache, "key1", String::class.java)

    assertThat(result).isEqualTo("value1")
  }

  @Test
  fun `normal get returns null for missing key`() {
    whenever(cache.get("missing", String::class.java)).thenReturn(null)

    val result = accessor.get(cache, "missing", String::class.java)

    assertThat(result).isNull()
  }

  @Test
  fun `RedisException is caught and returns null`() {
    whenever(cache.get(eq("bad"), eq(String::class.java)))
      .thenThrow(RedisException("Buffer underflow"))

    val result = accessor.get(cache, "bad", String::class.java)

    assertThat(result).isNull()
  }

  @Test
  fun `RedisException triggers eviction of bad entry`() {
    whenever(cache.get(eq("bad"), eq(String::class.java)))
      .thenThrow(RedisException("Buffer underflow"))

    accessor.get(cache, "bad", String::class.java)

    verify(cache).evictIfPresent("bad")
  }

  @Test
  fun `non-Redis exceptions propagate`() {
    whenever(cache.get(eq("key"), eq(String::class.java)))
      .thenThrow(IllegalStateException("something else"))

    assertThrows<IllegalStateException> {
      accessor.get(cache, "key", String::class.java)
    }
  }
}
