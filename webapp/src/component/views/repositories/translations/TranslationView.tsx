import * as React from 'react';
import {RepositoryLanguageProvider} from "../../../../hooks/RepositoryLanguagesProvider";
import {TranslationsGrid} from "../../../Translations/TranslationsGrid";
import {TranslationGridContextProvider} from "../../../Translations/TtranslationsGridContextProvider";
import {LINKS} from "../../../../constants/links";
import {TranslationCreationDialog} from "../../../Translations/TranslationCreationDialog";
import {Route} from 'react-router-dom';

export default function TranslationView() {
    return (
        <RepositoryLanguageProvider>
            <TranslationGridContextProvider>
                <TranslationsGrid/>
                <Route path={LINKS.REPOSITORY_TRANSLATIONS_ADD.template}>
                    <TranslationCreationDialog/>
                </Route>
            </TranslationGridContextProvider>
        </RepositoryLanguageProvider>
    );
}
