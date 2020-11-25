package io.polygloat.dtos.request;

import io.polygloat.model.Language;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LanguageDTO {
    @Getter
    private Long id;

    @Getter
    @Setter
    @NotBlank
    @Size(max = 100)
    private String name;

    @Getter
    @Setter
    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^[^,]*$", message = "can not contain coma")
    private String abbreviation;

    public LanguageDTO(@NotBlank String name, @NotBlank String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public static LanguageDTO fromEntity(Language language) {
        LanguageDTO languageDTO = new LanguageDTO(language.getName(), language.getAbbreviation());
        languageDTO.id = language.getId();
        return languageDTO;
    }
}
