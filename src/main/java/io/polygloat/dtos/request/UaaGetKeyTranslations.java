package io.polygloat.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UaaGetKeyTranslations {
    private String key;

    public String getKey() {
        return key;
    }
}
