import * as React from 'react';
import {useContext, useEffect} from 'react';
import Dialog from '@material-ui/core/Dialog';
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import {useRepository} from "../../hooks/useRepository";
import {LINKS, PARAMS} from '../../constants/links';
import {container} from "tsyringe";
import {RedirectionActions} from "../../store/global/redirectionActions";
import {StandardForm} from "../common/form/StandardForm";
import {LanguagesMenu} from "../common/form/LanguagesMenu";
import {TranslationActions} from "../../store/repository/TranslationActions";
import {TextField} from "../common/form/fields/TextField";
import {ResourceErrorComponent} from "../common/form/ResourceErrorComponent";
import {messageService} from "../../service/messageService";
import {Validation} from "../../constants/GlobalValidationSchema";
import {TranslationListContext} from "./TtranslationsGridContextProvider";
import {useTranslate} from "@polygloat/react";

export type TranslationCreationValue = {
    key: string;
    translations: { [abbreviation: string]: string }
}

const redirectionActions = container.resolve(RedirectionActions);
const translationActions = container.resolve(TranslationActions);
const messaging = container.resolve(messageService);

export function TranslationCreationDialog() {

    const repositoryDTO = useRepository();

    const t = useTranslate();

    let selectedLanguages = translationActions.useSelector(s => s.selectedLanguages);

    let saveLoadable = translationActions.useSelector(s => s.loadables.createKey);

    let listContext = useContext(TranslationListContext);

    function onClose() {
        translationActions.loadableReset.createKey.dispatch();
        redirectionActions.redirect.dispatch(LINKS.REPOSITORY_TRANSLATIONS.build({[PARAMS.REPOSITORY_ID]: repositoryDTO.id}));
    }

    useEffect(() => {
        if (saveLoadable.loaded && !saveLoadable.error) {
            messaging.success(t("translation_grid_translation_created"));
            listContext.loadData();
            onClose();
        }
    }, [saveLoadable.error, saveLoadable.loaded]);

    function onSubmit(v) {
        translationActions.loadableActions.createKey.dispatch(repositoryDTO.id, v);
    }

    const initialTranslations = selectedLanguages.reduce((res, l) => ({...res, [l]: ''}), {});

    return (
        <Dialog
            open
            onClose={() => onClose()}
            aria-labelledby="alert-dialog-title"
            aria-describedby="alert-dialog-description"
            fullWidth
        >
            <DialogTitle id="alert-dialog-title">{t('add_translation_dialog_title')}</DialogTitle>
            <DialogContent>
                {saveLoadable && saveLoadable.error && <ResourceErrorComponent error={saveLoadable.error}/>}

                <LanguagesMenu context="creation-dialog"/>
                <StandardForm onSubmit={onSubmit}
                              initialValues={{key: "", translations: initialTranslations}}
                              validationSchema={Validation.KEY_TRANSLATION_CREATION(selectedLanguages)}
                              onCancel={() => onClose()}>
                    <TextField multiline name="key" label={t("translation_grid_key_text")} fullWidth/>

                    {selectedLanguages.map(s => (
                        <TextField multiline key={s} name={"translations." + s} label={s}/>
                    ))}

                </StandardForm>
            </DialogContent>
        </Dialog>
    );
}
