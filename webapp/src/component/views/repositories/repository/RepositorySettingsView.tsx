import * as React from 'react';
import {FunctionComponent, useEffect, useState} from 'react';
import {useSelector} from 'react-redux';
import {AppState} from '../../../../store';
import {container} from 'tsyringe';
import {RepositoryActions} from '../../../../store/repository/RepositoryActions';
import {LINKS} from '../../../../constants/links';
import {Redirect} from 'react-router-dom';
import * as Yup from 'yup';
import {TextField} from '../../../common/form/fields/TextField';
import {BaseFormView} from '../../../layout/BaseFormView';
import {useRepository} from "../../../../hooks/useRepository";
import {Button} from "@material-ui/core";
import {confirmation} from "../../../../hooks/confirmation";
import {T} from "@polygloat/react";
import {ConfirmationDialogProps} from "../../../common/ConfirmationDialog";

const actions = container.resolve(RepositoryActions);

type ValueType = {
    name: string,
}

export const RepositorySettingsView: FunctionComponent = () => {

    const loadable = useSelector((state: AppState) => state.repositories.loadables.editRepository);
    const saveLoadable = useSelector((state: AppState) => state.repositories.loadables.editRepository);

    let repository = useRepository();

    let confirm = (options: ConfirmationDialogProps) => confirmation({title: <T>delete_repository_dialog_title</T>, ...options});

    const onSubmit = (values) => {
        actions.loadableActions.editRepository.dispatch(repository.id, values);
    };

    useEffect(() => {
        if (saveLoadable.touched) {
            actions.loadableReset.repository.dispatch();
        }
        return () => actions.loadableReset.editRepository.dispatch();
    }, [saveLoadable.touched]);


    useEffect(() => {
        return () => {
            actions.loadableReset.deleteRepository.dispatch();
        }
    }, []);

    const initialValues: ValueType = {name: repository.name};

    const [cancelled, setCancelled] = useState(false);

    if (cancelled) {
        return <Redirect to={LINKS.REPOSITORIES.build()}/>
    }

    return (
        <BaseFormView lg={6} md={8} title={<T>repository_settings_title</T>} initialValues={initialValues} onSubmit={onSubmit}
                      onCancel={() => setCancelled(true)}
                      saveActionLoadable={loadable}
                      validationSchema={Yup.object().shape(
                          {
                              name: Yup.string().required().min(3).max(100)
                          })}
                      customActions={
                          <Button color="secondary" variant="outlined" onClick={() => {
                              confirm({
                                  message: <T parameters={{name: repository.name}}>delete_repository_confirmation_message</T>,
                                  onConfirm: () => actions.loadableActions.deleteRepository.dispatch(repository.id),
                                  hardModeText: repository.name.toUpperCase()
                              })
                          }}><T>delete_repository_button</T></Button>
                          }
            >
            <TextField label={<T>repository_settings_name_label</T>} name="name" required={true}/>
            </BaseFormView>
    );
};