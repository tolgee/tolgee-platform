import {default as React, FunctionComponent, useEffect} from 'react';
import {container} from 'tsyringe';

import {useSelector} from "react-redux";
import {useHistory} from 'react-router-dom';

import {T} from "@tolgee/react";
import {AppState} from "../../../store";
import {BaseFormView} from "../../layout/BaseFormView";
import {Validation} from "../../../constants/GlobalValidationSchema";
import {SetPasswordFields} from "../../security/SetPasswordFields";
import {UserActions} from "../../../store/global/UserActions";
import {UserUpdateDTO} from "../../../service/request.types";
import {TextField} from "../../common/form/fields/TextField";
import {BaseUserSettingsView} from "./BaseUserSettingsView";
import {StandardForm} from "../../common/form/StandardForm";

const actions = container.resolve(UserActions);
const userActions = container.resolve(UserActions);

export const UserSettings: FunctionComponent = () => {

    let saveLoadable = useSelector((state: AppState) => state.user.loadables.updateUser);
    let resourceLoadable = useSelector((state: AppState) => state.user.loadables.userData);

    useEffect(() => {
        if (saveLoadable.loaded) {
            userActions.loadableActions.userData.dispatch();
        }
    }, [saveLoadable.loading]);

    const history = useHistory();

    return (
        <BaseUserSettingsView title={<T>User settings title</T>} loading={resourceLoadable.loading}>
            <StandardForm saveActionLoadable={saveLoadable}
                          initialValues={{
                              password: '',
                              passwordRepeat: '',
                              name: resourceLoadable.data!.name,
                              email: resourceLoadable.data!.username
                          } as UserUpdateDTO}
                          validationSchema={Validation.USER_SETTINGS}
                          onCancel={() => history.goBack()}
                          onSubmit={(v: UserUpdateDTO) => {
                              if (!v.password) {
                                  delete v.password;
                              }
                              actions.loadableActions.updateUser.dispatch(v);
                          }}>

                <TextField name="name" label={<T>User settings - Full name</T>}/>
                <TextField name="email" label={<T>User settings - E-mail</T>}/>
                <SetPasswordFields/>
            </StandardForm>
        </BaseUserSettingsView>
    );
};
