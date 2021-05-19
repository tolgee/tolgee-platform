import {singleton} from 'tsyringe';
import {AbstractLoadableActions, StateWithLoadables} from "../AbstractLoadableActions";
import {ImportExportService} from "../../service/ImportExportService";
import {AppState} from "../index";
import {useSelector} from 'react-redux';
import {ApiSchemaHttpService} from "../../service/http/ApiSchemaHttpService";
import {T} from "@tolgee/react";
import {components} from "../../service/apiSchema";

export class ImportState extends StateWithLoadables<ImportActions> {
    result?: components["schemas"]["PagedModelImportLanguageModel"] = undefined
    /**
     * Whether user already tried to apply import (Import button clicked)
     **/
    applyTouched?: Boolean
}

@singleton()
export class ImportActions extends AbstractLoadableActions<ImportState> {
    constructor(private service: ImportExportService, private schemaService: ApiSchemaHttpService) {
        super(new ImportState());
    }

    resetResult = this.createAction("RESET_RESULT").build.on((state) => {
        return {...state, result: undefined}
    })

    touchApply = this.createAction("TOUCH_APPLY").build.on((state) => {
        return {...state, applyTouched: true}
    })

    loadableDefinitions = {
        cancelImport: this.createLoadableDefinition(
            this.schemaService.schemaRequest(
                "/v2/repositories/{repositoryId}/import",
                "delete"
            )
        ),
        conflicts: this.createLoadableDefinition(
            this.schemaService.schemaRequest(
                "/v2/repositories/{repositoryId}/import/result/languages/{languageId}/translations",
                "get"
            )
        ),
        translations: this.createLoadableDefinition(
            this.schemaService.schemaRequest(
                "/v2/repositories/{repositoryId}/import/result/languages/{languageId}/translations",
                "get"
            )
        ),
        deleteLanguage: this.createLoadableDefinition(
            this.schemaService.schemaRequest(
                "/v2/repositories/{repositoryId}/import/result/languages/{languageId}",
                "delete"
            ), undefined, <T>import_language_deleted</T>
        ),
        resolveTranslationConflictOverride: this.createLoadableDefinition(
            this.schemaService.schemaRequest(
                "/v2/repositories/{repositoryId}/import/result/languages/{languageId}/translations/{translationId}/resolve/set-override",
                "put"
            )),
        resolveTranslationConflictKeep: this.createLoadableDefinition(
            this.schemaService.schemaRequest(
                "/v2/repositories/{repositoryId}/import/result/languages/{languageId}/translations/{translationId}/resolve/set-keep-existing",
                "put"
            )),
        resolveAllOverride: this.createLoadableDefinition(
            this.schemaService.schemaRequest(
                "/v2/repositories/{repositoryId}/import/result/languages/{languageId}/resolve-all/set-override", "put"
            ), undefined, <T>import_resolve_override_all_success</T>
        ),
        resolveAllKeepExisting: this.createLoadableDefinition(
            this.schemaService.schemaRequest(
                "/v2/repositories/{repositoryId}/import/result/languages/{languageId}/resolve-all/set-keep-existing", "put"
            ), undefined, <T>import_resolve_keep_all_existing_success</T>
        ),
        applyImport: this.createLoadableDefinition(
            this.schemaService.schemaRequest(
                "/v2/repositories/{repositoryId}/import/apply", "put"
            ), undefined, <T>import_successfully_applied_message</T>
        ),
        selectLanguage: this.createLoadableDefinition(
            this.schemaService.schemaRequest(
                "/v2/repositories/{repositoryId}/import/result/languages/{importLanguageId}/select-existing/{existingLanguageId}", "put"
            )
        ),
        addFiles: this.createLoadableDefinition(
            this.schemaService.schemaRequest("/v2/repositories/{repositoryId}/import", "post"),
            (state, action): ImportState => {
                return {...state, result: action.payload.result}
            }
        ),
        getResult: this.createLoadableDefinition(
            this.schemaService.schemaRequest("/v2/repositories/{repositoryId}/import/result", "get", {
                disableNotFoundHandling: true
            }),
            (state, action): ImportState => {
                return {...state, result: action.payload}
            }
        ),
        getFileIssues: this.createLoadableDefinition(
            this.schemaService.schemaRequest("/v2/repositories/{repositoryId}/import/result/files/{importFileId}/issues", "get")
        ),
    };

    get prefix(): string {
        return 'IMPORT';
    }

    useSelector<T>(selector: (state: ImportState) => T): T {
        return useSelector((state: AppState) => selector(state.import))
    }
}
