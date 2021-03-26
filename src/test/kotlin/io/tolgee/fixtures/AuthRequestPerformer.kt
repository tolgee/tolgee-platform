package io.tolgee.fixtures

import org.springframework.test.web.servlet.ResultActions

interface AuthRequestPerformer : RequestPerformer {
    fun performAuthPut(url: String, content: Any?): ResultActions
    fun performAuthPost(url: String, content: Any?): ResultActions
    fun performAuthGet(url: String): ResultActions
    fun performAuthDelete(url: String, content: Any?): ResultActions
}
