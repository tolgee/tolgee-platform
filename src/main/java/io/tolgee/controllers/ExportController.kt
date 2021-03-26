package io.tolgee.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tolgee.collections.LanguageSet;
import io.tolgee.model.Permission;
import io.tolgee.model.Repository;
import io.tolgee.service.LanguageService;
import io.tolgee.service.SecurityService;
import io.tolgee.service.TranslationService;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/repository/{repositoryId}/export")
public class ExportController implements IController {

    private final TranslationService translationService;
    private final SecurityService securityService;
    private final LanguageService languageService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ExportController(TranslationService translationService, SecurityService securityService, LanguageService languageService, ObjectMapper objectMapper) {
        this.translationService = translationService;
        this.securityService = securityService;
        this.languageService = languageService;
        this.objectMapper = objectMapper;
    }


    @GetMapping(value = "/jsonZip", produces = "application/zip")
    public ResponseEntity<StreamingResponseBody> doExportJsonZip(@PathVariable("repositoryId") Long repositoryId) {
        Repository repository = securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.VIEW).getRepository();
        LanguageSet languages = languageService.findAll(repositoryId);
        return ResponseEntity
                .ok()
                .header("Content-Disposition", String.format("attachment; filename=\"%s.zip\"", repository.getName()))
                .body(out -> {
                    var zipOutputStream = new ZipOutputStream(out);

                    Map<String, Object> translations = translationService.getTranslations(languages.getAbbreviations(), repositoryId);

                    for (Map.Entry<String, Object> entry : translations.entrySet()) {
                        zipOutputStream.putNextEntry(new ZipEntry(String.format("%s.json", entry.getKey())));

                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(objectMapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsBytes(entry.getValue()));

                        IOUtils.copy(byteArrayInputStream, zipOutputStream);
                        byteArrayInputStream.close();
                        zipOutputStream.closeEntry();
                    }

                    zipOutputStream.close();
                });
    }

}
