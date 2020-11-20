package io.polygloat.dtos.response;

import io.polygloat.dtos.query_results.KeyDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyResponseDTO {
    private Long id;

    private String name;

    private Map<String, String> translations = new LinkedHashMap<>();

    public static KeyResponseDTO fromQueryResult(KeyDTO keyDTO) {
        return new KeyResponseDTO(keyDTO.getId(), keyDTO.getPath().getFullPathString(), keyDTO.getTranslations());
    }
}
