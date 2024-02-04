package io.tolgee.unit.formats.ios.out

import io.tolgee.formats.ios.out.IcuToIOsMessageConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class IcuToIOsMessageConvertorTest {
  @Test
  fun `converts # to li when plural`() {
    val result = IcuToIOsMessageConvertor("{param, plural, other {# dogs}}").convert()
    result.formsResult!!["other"]!!.assert.isEqualTo("%li dogs")
  }

  @Test
  fun `converts param to @`() {
    val result = IcuToIOsMessageConvertor("hello {name}").convert()
    result.singleResult.assert.isEqualTo("hello %@")
  }
}
