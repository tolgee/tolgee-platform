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

package io.tolgee.unit.component

import io.tolgee.component.TolgeeCacheErrorHandler
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.redisson.client.RedisException
import org.springframework.cache.Cache

class TolgeeCacheErrorHandlerTest {
  private val cache = mock<Cache>()
  private val handler = TolgeeCacheErrorHandler()

  @BeforeEach
  fun setup() {
    whenever(cache.name).thenReturn("testCache")
  }

  @Test
  fun `RedisException evicts the bad entry and is suppressed`() {
    handler.handleCacheGetError(RedisException("Buffer underflow"), cache, "bad")

    verify(cache).evictIfPresent("bad")
  }

  @Test
  fun `non-Redis exceptions propagate and do not evict`() {
    val exception = IllegalStateException("something else")

    assertThrows<IllegalStateException> {
      handler.handleCacheGetError(exception, cache, "key")
    }.assert.isSameAs(exception)

    verify(cache, never()).evictIfPresent("key")
  }
}
