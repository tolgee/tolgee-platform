import React, {ChangeEvent, FunctionComponent} from 'react';
import {FormControl, InputLabel, MenuItem, Select} from "@material-ui/core";
import {useRepositoryLanguages} from "../../../../../hooks/useRepositoryLanguages";
import {T} from "@tolgee/react";
import {container} from "tsyringe";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import {useImportDataHelper} from "../hooks/useImportDataHelper";
import {useRepository} from "../../../../../hooks/useRepository";

const actions = container.resolve(ImportActions)
export const ImportRowLanguageMenu: FunctionComponent<{
    value?: number,
    importLanguageId: number
}> = (props) => {
    const languages = useRepositoryLanguages()
    const importData = useImportDataHelper()
    const usedLanguages = importData.result!._embedded!.languages!.map(l => l.existingLanguageId).filter(l => !!l)
    const repository = useRepository()

    const onChange = (changeEvent: ChangeEvent<any>) => {
        actions.loadableActions.selectLanguage.dispatch({
            path: {
                repositoryId: repository.id,
                importLanguageId: props.importLanguageId,
                existingLanguageId: changeEvent.target.value
            }
        })
    }

    return (
        <>
            <FormControl fullWidth>
                <InputLabel shrink id="import_row_language_select">
                    <T>import_language_select</T>
                </InputLabel>
                <Select
                    labelId="import_row_language_select"
                    value={props.value}
                    onChange={onChange}
                    fullWidth
                >
                    {languages.filter(lang => props.value == lang.id || usedLanguages.indexOf(lang.id) < 0).map(l => <MenuItem value={l.id}>
                        {l.name}
                    </MenuItem>)}
                </Select>
            </FormControl>
        </>
    );
};

