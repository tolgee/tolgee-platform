import {default as React, FunctionComponent} from 'react';
import {DashboardPage} from '../layout/DashboardPage';
import {Button} from '@material-ui/core';
import {useSelector} from 'react-redux';
import {AppState} from '../../store';
import {LINKS} from '../../constants/links';
import {Redirect} from 'react-router-dom';
import Box from '@material-ui/core/Box';
import {container} from 'tsyringe';
import {SetPasswordFields} from './SetPasswordFields';
import {SignUpActions} from '../../store/global/signUpActions';
import {TextField} from '../common/form/fields/TextField';
import {useConfig} from "../../hooks/useConfig";
import {Validation} from "../../constants/GlobalValidationSchema";
import {BaseFormView} from "../layout/BaseFormView";
import {Alert} from "../common/Alert";
import {T} from '@polygloat/react';
import {BaseView} from "../layout/BaseView";

const actions = container.resolve(SignUpActions);

export type SignUpType = {
    name: string,
    email: string,
    password: string;
    passwordRepeat: string;
    invitationCode?: string;
}

const SignUpView: FunctionComponent = () => {
    const security = useSelector((state: AppState) => state.global.security);
    const state = useSelector((state: AppState) => state.signUp.loadables.signUp);
    const config = useConfig();
    const remoteConfig = useConfig();

    if (!remoteConfig.authentication || security.allowPrivate || !security.allowRegistration) {
        return (<Redirect to={LINKS.AFTER_LOGIN.build()}/>);
    }

    return (
        <DashboardPage>
            {state.loaded && config.needsEmailVerification ?
                <BaseView title={<T>sign_up_success_title</T>} lg={4} md={6} xs={12}>
                    <Alert severity="success"><T>sign_up_success_needs_verification_message</T></Alert>
                </BaseView>
                :
                <BaseFormView loading={state.loading} title={<T>sign_up_title</T>} lg={4} md={6} xs={12} saveActionLoadable={state}
                              initialValues={{password: '', passwordRepeat: '', name: '', email: ''} as SignUpType}
                              validationSchema={Validation.SIGN_UP}
                              submitButtons={
                                  <Box display="flex" justifyContent="flex-end">
                                      <Button color="primary" type="submit"><T>sign_up_submit_button</T></Button>
                                  </Box>
                              }
                              onSubmit={(v: SignUpType) => {
                                  actions.loadableActions.signUp.dispatch(v);
                              }}>
                    <TextField name="name" label={<T>sign_up_form_full_name</T>}/>
                    <TextField name="email" label={<T>sign_up_form_email</T>}/>
                    <SetPasswordFields/>
                </BaseFormView>}
        </DashboardPage>
    );
};

export default SignUpView;
