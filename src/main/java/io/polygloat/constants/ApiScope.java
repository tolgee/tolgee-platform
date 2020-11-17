package io.polygloat.constants;

import io.polygloat.exceptions.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public enum ApiScope {
    TRANSLATIONS_VIEW("translations.view"),
    TRANSLATIONS_EDIT("translations.edit"),
    SOURCES_EDIT("sources.edit");

    @Setter
    private String value;

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
}
