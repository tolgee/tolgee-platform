package com.polygloat.Assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.IntegerAssert;
import org.assertj.core.api.StringAssert;

import java.util.Map;

public class StandardValidationMessageAssert extends AbstractAssert<StandardValidationMessageAssert, Map<String, String>> {

    public StandardValidationMessageAssert(Map<String, String> data) {
        super(data, StandardValidationMessageAssert.class);
    }

    public StringAssert onField(String field) {
        if (!actual.containsKey(field)) {
            failWithMessage("Error is not on field %s.", field);
        }
        return new StringAssert(actual.get(field)).describedAs("Message assertion on field %s", field);
    }
    public IntegerAssert errorCount() {
        return new IntegerAssert(actual.size());
    }


}
