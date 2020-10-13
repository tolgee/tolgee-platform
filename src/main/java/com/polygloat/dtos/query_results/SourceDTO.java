package com.polygloat.dtos.query_results;

import com.polygloat.dtos.PathDTO;
import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class SourceDTO {
    @Getter
    private PathDTO path;

    @Getter
    private Long id;

    @Getter
    private Map<String, String> translations = new LinkedHashMap<>();

    public SourceDTO(Object[] queryResult) {
        LinkedList<Object> data = new LinkedList<>(Arrays.asList(queryResult));
        this.id = (Long) data.removeFirst();
        this.path = PathDTO.fromFullPath((String) data.removeFirst());

        for (int i = 0; i < data.size(); i = i + 2) {
            String key = (String) data.get(i);
            String value = (String) data.get(i + 1);

            //remove not existing langs or folders
            if (key == null) {
                continue;
            }

            this.translations.put(key, value);
        }
    }
}
