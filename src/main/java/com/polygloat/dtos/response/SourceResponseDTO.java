package com.polygloat.dtos.response;

import com.polygloat.dtos.query_results.SourceDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceResponseDTO {
    private Long id;

    private String name;

    private Map<String, String> translations = new LinkedHashMap<>();

    public static SourceResponseDTO fromQueryResult(SourceDTO sourceDTO) {
        return new SourceResponseDTO(sourceDTO.getId(), sourceDTO.getPath().getFullPathString(), sourceDTO.getTranslations());
    }
}
