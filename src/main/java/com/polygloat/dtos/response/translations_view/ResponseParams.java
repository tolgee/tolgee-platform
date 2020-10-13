package com.polygloat.dtos.response.translations_view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseParams {
    private String search;
    private Set<String> languages;
}
