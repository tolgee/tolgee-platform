package com.polygloat.Assertions;


import com.polygloat.helpers.JsonHelper;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Builder
@Data
public class UserApiAppAction {
    HttpMethod method;

    Object body;

    String apiKey;

    String url;

    HttpStatus expectedStatus;

    public RequestBuilder getRequestBuilder() {
        String url = this.getUrl() + "?ak=" + apiKey;

        if (this.getMethod() == null) {
            method = HttpMethod.GET;
        }

        switch (this.getMethod()) {
            case PUT:
                return withContent(MockMvcRequestBuilders.put(url));
            case POST:
                return withContent(MockMvcRequestBuilders.post(url));
            case DELETE:
                return withContent(MockMvcRequestBuilders.delete(url));
            default:
                return withContent(MockMvcRequestBuilders.get(url));
        }
    }

    private RequestBuilder withContent(MockHttpServletRequestBuilder builder) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(JsonHelper.asJsonString(this.body));
    }

}
