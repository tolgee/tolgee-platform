/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component

import com.unboundid.util.Base64
import io.tolgee.assertions.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.annotations.Test

@SpringBootTest(properties = ["tolgee.authentication.jwtSecret=this_is_dummy_jwt_secret"])
class AesTest : AbstractTestNGSpringContextTests() {

    @set:Autowired
    lateinit var aes: Aes

    @Test
    fun encrypt() {
        assertThat(Base64.encode(aes.encrypt("hello".toByteArray(charset("UTF-8")))))
                .isEqualTo("17u71OZauaAoEf+e0vc6Kg==")
    }

    @Test
    fun decrypt() {
        val base64 = Base64.decode("17u71OZauaAoEf+e0vc6Kg==")
        assertThat(aes.decrypt(base64).toString(charset("UTF-8"))).isEqualTo("hello")
    }
}