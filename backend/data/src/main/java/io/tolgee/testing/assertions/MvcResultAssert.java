package io.tolgee.testing.assertions;

import org.assertj.core.api.AbstractAssert;
import org.springframework.test.web.servlet.MvcResult;

public class MvcResultAssert extends AbstractAssert<MvcResultAssert, MvcResult> {

    public MvcResultAssert(MvcResult mvcResult) {
        super(mvcResult, MvcResultAssert.class);
    }

    public ErrorResponseAssert error() {
        return new ErrorResponseAssert(actual);
    }
}
