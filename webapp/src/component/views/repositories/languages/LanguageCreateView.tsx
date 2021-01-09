import * as React from 'react';
import {useState} from 'react';
import {container} from 'tsyringe';
import {LINKS, PARAMS} from '../../../../constants/links';
import {Route, Switch, useRouteMatch} from 'react-router-dom';
import {TextField} from '../../../common/form/fields/TextField';
import {BaseFormView} from '../../../layout/BaseFormView';
import {LanguageActions} from '../../../../store/languages/LanguageActions';
import {LanguageDTO} from "../../../../service/response.types";
import {useRedirect} from "../../../../hooks/useRedirect";
import {Validation} from "../../../../constants/GlobalValidationSchema";

const actions = container.resolve(LanguageActions);

export const LanguageCreateView = () => {
    let match = useRouteMatch();

    const repositoryId = match.params[PARAMS.REPOSITORY_ID];

    const [cancelled, setCancelled] = useState(false);

    let createLoadable = actions.useSelector(s => s.loadables.create);

    const onSubmit = (values) => {
        const dto: LanguageDTO = {
            ...values,
        };
        actions.loadableActions.create.dispatch(repositoryId, dto);
    };

    if (createLoadable.loaded || cancelled) {
        setCancelled(false);
        actions.loadableReset.create.dispatch();
        useRedirect(LINKS.REPOSITORY_LANGUAGES, {[PARAMS.REPOSITORY_ID]: repositoryId});
    }

    return (
        <>
            <BaseFormView
                lg={6} md={8} xs={12}
                title={'Add language'}
                initialValues={{name: "", abbreviation: ""}}
                onSubmit={onSubmit}
                onCancel={() => setCancelled(true)}
                saveActionLoadable={createLoadable}
                validationSchema={Validation.LANGUAGE}
            >
                <>
                    <TextField label="Name" name="name" required={true}/>
                    <TextField label="Abbreviation" name="abbreviation" required={true}/>
                </>
            </BaseFormView>
            <Switch>
                <Route exact path={LINKS.REPOSITORY_TRANSLATIONS_ADD.template}>
                </Route>
            </Switch>
        </>
    );
};
