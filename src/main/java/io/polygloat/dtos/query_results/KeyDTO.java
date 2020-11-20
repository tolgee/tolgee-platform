package io.polygloat.dtos.query_results;

import io.polygloat.dtos.PathDTO;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class KeyDTO {
    private PathDTO path;

    private Long id;

    private Map<String, String> translations = new LinkedHashMap<>();

    public KeyDTO(Object[] queryResult) {
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

    public PathDTO getPath() {
        return this.path;
    }

    public Long getId() {
        return this.id;
    }

    public Map<String, String> getTranslations() {
        return this.translations;
    }
}
