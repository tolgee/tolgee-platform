package io.polygloat.assertions

import io.polygloat.helpers.JsonHelper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

class UserApiAppAction(
        var method: HttpMethod? = null,
        var body: Any? = null,
        var apiKey: String? = null,
        var url: String? = null,
        var expectedStatus: HttpStatus? = null,
) {
    val requestBuilder: RequestBuilder
        get() {
            val url: String = this.url + "?ak=" + apiKey
            if (this.method == null) {
                method = HttpMethod.GET
            }
            return when (this.method) {
                HttpMethod.PUT -> withContent(MockMvcRequestBuilders.put(url))
                HttpMethod.POST -> withContent(MockMvcRequestBuilders.post(url))
                HttpMethod.DELETE -> withContent(MockMvcRequestBuilders.delete(url))
                else -> withContent(MockMvcRequestBuilders.get(url))
            }
        }

    private fun withContent(builder: MockHttpServletRequestBuilder): RequestBuilder {
        return builder.contentType(MediaType.APPLICATION_JSON).content(JsonHelper.asJsonString(body))
    }
}