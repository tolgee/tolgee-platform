import React, {ChangeEvent, FunctionComponent} from 'react';
import {Box, FormControl, FormHelperText, InputLabel, MenuItem, Select, Typography} from "@material-ui/core";
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
    const applyTouched = actions.useSelector(s => s.applyTouched)

    const onChange = (changeEvent: ChangeEvent<any>) => {
        actions.loadableActions.selectLanguage.dispatch({
            path: {
                repositoryId: repository.id,
                importLanguageId: props.importLanguageId,
                existingLanguageId: changeEvent.target.value
            }
        })
    }

    const availableLanguages = languages.filter(lang => props.value == lang.id || usedLanguages.indexOf(lang.id) < 0)


    return (
        <>
            <FormControl fullWidth error={applyTouched && !props.value}>
                <InputLabel shrink id="import_row_language_select">
                    <T>import_language_select</T>
                </InputLabel>
                <Select
                    labelId="import_row_language_select"
                    value={props.value || ''}
                    onChange={onChange}
                    fullWidth

                >
                    {availableLanguages.length ?
                        availableLanguages.map(l =>
                            <MenuItem value={l.id} key={l.id}>
                                {l.name}
                            </MenuItem>)
                        :
                        <Box p={2}>
                            <Typography variant="body1"><T>import_no_languages_to_choose_from</T></Typography>
                        </Box>
                    }
                </Select>
                {(applyTouched && !props.value) &&
                <FormHelperText><T>import_existing_language_not_selected_error</T></FormHelperText>}
            </FormControl>
        </>
    );
};

