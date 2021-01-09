import * as React from 'react';
import {useEffect} from 'react';
import {container} from 'tsyringe';
import {LINKS, PARAMS} from '../../../../constants/links';
import {useRouteMatch} from 'react-router-dom';
import {TextField} from '../../../common/form/fields/TextField';
import {BaseFormView} from '../../../layout/BaseFormView';
import {LanguageActions} from '../../../../store/languages/LanguageActions';
import {Button} from "@material-ui/core";
import {confirmation} from "../../../../hooks/confirmation";
import {LanguageDTO} from "../../../../service/response.types";
import {Validation} from "../../../../constants/GlobalValidationSchema";
import {useRedirect} from "../../../../hooks/useRedirect";
import {T} from "@polygloat/react";
import {ConfirmationDialogProps} from "../../../common/ConfirmationDialog";

const actions = container.resolve(LanguageActions);

export const LanguageEditView = () => {

    let confirmationMessage = (options: ConfirmationDialogProps) => confirmation({title: "Delete language", ...options});

    let match = useRouteMatch();

    const repositoryId = match.params[PARAMS.REPOSITORY_ID];
    const languageId = match.params[PARAMS.LANGUAGE_ID];

    let languageLoadable = actions.useSelector(s => s.loadables.language);
    let editLoadable = actions.useSelector(s => s.loadables.edit);
    let deleteLoadable = actions.useSelector(s => s.loadables.delete);

    useEffect(() => {
        if (!languageLoadable.loaded && !languageLoadable.loading) {
            actions.loadableActions.language.dispatch(repositoryId, languageId);
        }
        return () => {
            actions.loadableReset.edit.dispatch();
            actions.loadableReset.language.dispatch();
        }
    }, []);


    useEffect(() => {
        if (deleteLoadable.loaded) {
            useRedirect(LINKS.REPOSITORY_LANGUAGES, {[PARAMS.REPOSITORY_ID]: repositoryId});
        }
        return () => actions.loadableReset.delete.dispatch();
    }, [deleteLoadable.loaded]);

    const onSubmit = (values) => {
        const dto: LanguageDTO = {
            ...values,
            id: languageId
        };
        actions.loadableActions.edit.dispatch(repositoryId, dto);
    };

    return (
        <BaseFormView
            lg={6} md={8} xs={10}
            title={<T>language_settings_title</T>}
            initialValues={languageLoadable.data}
            onSubmit={onSubmit}
            saveActionLoadable={editLoadable}
            resourceLoadable={languageLoadable}
            validationSchema={Validation.LANGUAGE}
            customActions={
                <Button variant="outlined" color="secondary"
                        onClick={() => confirmationMessage({
                            message: <T parameters={{name: languageLoadable.data.name}}>delete_language_confirmation</T>,
                            hardModeText: languageLoadable.data.name.toUpperCase(),
                            confirmButtonText: <T>global_delete_button</T>,
                            confirmButtonColor: "secondary",
                            onConfirm: () => {
                                actions.loadableActions.delete.dispatch(repositoryId, languageId)
                            }
                        })}
                >
                    <T>delete_language_button</T>
                </Button>}
        >
            {() => <>
                <TextField label={<T>edit_language_languge_name_label</T>} name="name" required={true}/>
                <TextField label={<T>abbreviation</T>} name="abbreviation" required={true}/>
            </>}
        </BaseFormView>
    );
};
