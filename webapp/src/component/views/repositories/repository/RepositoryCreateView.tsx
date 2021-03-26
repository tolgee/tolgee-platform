import * as React from 'react';
import {FunctionComponent, useState} from 'react';
import {useSelector} from 'react-redux';
import {AppState} from '../../../../store';
import {container} from 'tsyringe';
import {RepositoryActions} from '../../../../store/repository/RepositoryActions';
import {LanguageDTO} from '../../../../service/response.types';
import {LINKS} from '../../../../constants/links';
import {Redirect} from 'react-router-dom';
import {TextField} from '../../../common/form/fields/TextField';
import {BaseFormView} from '../../../layout/BaseFormView';
import {FieldArray} from "../../../common/form/fields/FieldArray";
import {Validation} from "../../../../constants/GlobalValidationSchema";
import {PossibleRepositoryPage} from "../../PossibleRepositoryPage";
import {T} from "@tolgee/react";

const actions = container.resolve(RepositoryActions);


type ValueType = {
    name: string,
    languages: Partial<LanguageDTO>[];
}

export const RepositoryCreateView: FunctionComponent = () => {

    const loadable = useSelector((state: AppState) => state.repositories.loadables.createRepository);

    const onSubmit = (values) => {
        actions.loadableActions.createRepository.dispatch(values);
    };

    const initialValues: ValueType = {name: '', languages: [{abbreviation: "", name: ""}]};

    const [cancelled, setCancelled] = useState(false);

    if (cancelled) {
        return <Redirect to={LINKS.REPOSITORIES.build()}/>
    }

    return (
        <PossibleRepositoryPage>
            <BaseFormView lg={6} md={8} title={<T>create_repository_view</T>} initialValues={initialValues} onSubmit={onSubmit}
                          onCancel={() => setCancelled(true)}
                          saveActionLoadable={loadable}
                          validationSchema={Validation.REPOSITORY_CREATION}
            >
                <>
                    <TextField label={<T>create_repository_name_label</T>} name="name" required={true}/>

                    <FieldArray name="languages">
                        {(n) => (
                            <>
                                <TextField fullWidth={false} label={<T>create_repository_language_name_label</T>} name={n('name')} required={true}/>
                                <TextField fullWidth={false} label={<T>create_repository_language_abbreviation_label</T>} name={n('abbreviation')} required={true}/>
                            </>
                        )}
                    </FieldArray>
                </>
            </BaseFormView>
        </PossibleRepositoryPage>
    );
};
