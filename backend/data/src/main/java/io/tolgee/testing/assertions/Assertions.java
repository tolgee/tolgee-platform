package io.tolgee.testing.assertions;

import org.springframework.test.web.servlet.MvcResult;

public class Assertions extends org.assertj.core.api.Assertions {
    public static MvcResultAssert assertThat(MvcResult mvcResult) {
        return new MvcResultAssert(mvcResult);
    }
}
