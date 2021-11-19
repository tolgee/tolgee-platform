package io.tolgee.testing.assertions;

import org.assertj.core.api.AbstractAssert;

import java.util.List;
import java.util.Map;

public class CustomValidationMessageAssert extends AbstractAssert<CustomValidationMessageAssert, Map<String, List<Object>>> {

    public CustomValidationMessageAssert(Map<String, List<Object>> data) {
        super(data, CustomValidationMessageAssert.class);
    }

    public CustomValidationMessageAssert hasMessage(String message) {
        if (!actual.containsKey(message)) {
            failWithMessage("Error has no message '%s'. Validation map is: '%s'", message, actual);
        }
        return this;
    }


}
