package io.polygloat.constants;

import io.polygloat.exceptions.NotFoundException;

public enum ApiScope {
    TRANSLATIONS_VIEW("translations.view"),
    TRANSLATIONS_EDIT("translations.edit"),
    KEYS_EDIT("keys.edit");

    private String value;

    private ApiScope(String value) {
        this.value = value;
    }

    public static ApiScope fromValue(String value) {
        for (ApiScope scope : ApiScope.values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new NotFoundException(Message.SCOPE_NOT_FOUND);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
