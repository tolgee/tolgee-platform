/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component

import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test

class AesTest {
  @Test
  fun `it works`() {
    val aes = Aes("this_is_dummy_jwt_secret_azeazezaezaezaezaezzaezaezaeazeazezaeazezeaeazea".toByteArray())
    val encrypted1 = aes.encrypt("hello".toByteArray(charset("UTF-8")))
    val encrypted2 = aes.encrypt("hello hello".toByteArray(charset("UTF-8")))
    val decrypted2 = aes.decrypt(encrypted2).toString(charset("UTF-8"))
    val decrypted1 = aes.decrypt(encrypted1).toString(charset("UTF-8"))
    assertThat(decrypted1).isEqualTo("hello")
    assertThat(decrypted2).isEqualTo("hello hello")
  }
}
