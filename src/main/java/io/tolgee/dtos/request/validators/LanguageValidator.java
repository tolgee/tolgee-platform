package io.tolgee.dtos.request.validators;

import io.tolgee.constants.Message;
import io.tolgee.dtos.request.LanguageDto;
import io.tolgee.dtos.request.validators.exceptions.ValidationException;
import io.tolgee.exceptions.NotFoundException;
import io.tolgee.model.Language;
import io.tolgee.model.Project;
import io.tolgee.service.LanguageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Optional;

@Component
public class LanguageValidator {
    private LanguageService languageService;

    @Autowired
    public LanguageValidator(LanguageService languageService) {
        this.languageService = languageService;
    }

    public void validateEdit(LanguageDto dto) {
        LinkedHashSet<ValidationError> validationErrors = new LinkedHashSet<>();

        //handle edit validation
        Language language = languageService.findById(dto.getId()).orElseThrow(NotFoundException::new);

        Project project = language.getProject();

        if (!language.getName().equals(dto.getName())) {
            validateNameUniqueness(dto, project).ifPresent(validationErrors::add);
        }
        if (!language.getTag().equals(dto.getTag())) {
            validateTagUniqueness(dto, project).ifPresent(validationErrors::add);
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }
    }

    public void validateCreate(LanguageDto dto, Project project) {
        LinkedHashSet<ValidationError> validationErrors = new LinkedHashSet<>();

        //handle create validation
        validateTagUniqueness(dto, project).ifPresent(validationErrors::add);
        validateNameUniqueness(dto, project).ifPresent(validationErrors::add);

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }
    }

    private Optional<ValidationError> validateNameUniqueness(LanguageDto dto, Project project) {
        if (languageService.findByName(dto.getName(), project).isPresent()) {
            return Optional.of(new ValidationError(ValidationErrorType.CUSTOM_VALIDATION, Message.LANGUAGE_NAME_EXISTS));
        }
        return Optional.empty();
    }

    private Optional<ValidationError> validateTagUniqueness(LanguageDto dto, Project project) {
        if (languageService.findByTag(dto.getTag(), project).isPresent()) {
            return Optional.of(new ValidationError(ValidationErrorType.CUSTOM_VALIDATION, Message.LANGUAGE_TAG_EXISTS));
        }
        return Optional.empty();
    }
}
