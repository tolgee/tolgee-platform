package io.tolgee.fixtures

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions

@Component
@Scope("prototype")
open class SignedInRequestPerformer: BaseRequestPerformer(), AuthRequestPerformer {

    override fun performAuthPut(url: String, content: Any?): ResultActions {
        return mvc.perform(LoggedRequestFactory.loggedPut(url).withJsonContent(content))
    }

    override fun performAuthPost(url: String, content: Any?): ResultActions {
        return mvc.perform(LoggedRequestFactory.loggedPost(url).withJsonContent(content))
    }

    override fun performAuthGet(url: String): ResultActions {
        return mvc.perform(LoggedRequestFactory.loggedGet(url))
    }

    override fun performAuthDelete(url: String, content: Any?): ResultActions {
        return mvc.perform(LoggedRequestFactory.loggedDelete(url).withJsonContent(content))
    }
}
