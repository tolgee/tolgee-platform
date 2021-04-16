package io.tolgee.controllers.publicController

import io.tolgee.controllers.AbstractControllerTest
import org.testng.annotations.Test

class SignUpTest : AbstractControllerTest() {

    @Test
    open fun testSignUpGithubRegistrationsNotAllowed(){
        tolgeeProperties.authentication.registrationsAllowed = false

    }
}
