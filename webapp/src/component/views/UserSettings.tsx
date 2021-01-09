import {default as React, FunctionComponent, useEffect} from 'react';
import {container} from 'tsyringe';
import {TextField} from '../common/form/fields/TextField';
import {Validation} from "../../constants/GlobalValidationSchema";
import {BaseFormView} from "../layout/BaseFormView";
import {SetPasswordFields} from "../security/SetPasswordFields";
import {UserActions} from "../../store/global/userActions";
import {UserUpdateDTO} from "../../service/response.types";
import {useSelector} from "react-redux";
import {AppState} from "../../store";
import {useHistory} from 'react-router-dom';
import {PossibleRepositoryPage} from "./PossibleRepositoryPage";
import {T} from "@polygloat/react";

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
        <PossibleRepositoryPage>
            <BaseFormView title={<T>User settings title</T>} lg={6} md={8} xs={12} saveActionLoadable={saveLoadable} resourceLoadable={resourceLoadable}
                          initialValues={{password: '', passwordRepeat: '', name: resourceLoadable.data.name, email: resourceLoadable.data.username}}
                          validationSchema={Validation.USER_SETTINGS}
                          submitButtonInner={"Save"}
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
            </BaseFormView>
        </PossibleRepositoryPage>
    );
};