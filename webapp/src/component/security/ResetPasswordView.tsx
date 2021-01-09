import {default as React, FunctionComponent, useEffect} from 'react';
import {DashboardPage} from '../layout/DashboardPage';
import {BaseView} from '../layout/BaseView';
import {Button} from '@material-ui/core';
import {useSelector} from 'react-redux';
import {AppState} from '../../store';
import {LINKS} from '../../constants/links';
import {Redirect} from 'react-router-dom';
import {StandardForm} from '../common/form/StandardForm';
import {TextField} from '../common/form/fields/TextField';
import Box from '@material-ui/core/Box';
import {container} from 'tsyringe';
import {GlobalActions} from '../../store/global/globalActions';
import {Alert} from '../common/Alert';
import {useConfig} from "../../hooks/useConfig";
import {Validation} from "../../constants/GlobalValidationSchema";


interface LoginProps {

}

const globalActions = container.resolve(GlobalActions);

type ValueType = {
    email: string;
}

const PasswordResetView: FunctionComponent<LoginProps> = (props) => {

    const security = useSelector((state: AppState) => state.global.security);
    const remoteConfig = useConfig();

    const loadable = useSelector((state: AppState) => state.global.loadables.resetPasswordRequest);

    if (!remoteConfig.authentication || security.allowPrivate || !remoteConfig.passwordResettable) {
        return (<Redirect to={LINKS.AFTER_LOGIN.build()}/>);
    }

    useEffect(() => () => globalActions.loadableReset.resetPasswordRequest.dispatch(), []);

    return (
        <DashboardPage>
            <BaseView title="Reset password" lg={6} md={8} xs={12} loading={loadable.loading}>
                {loadable.error || loadable.loaded &&
                <Box mt={1}>
                    {loadable.loaded && <Alert severity="success">Request successfully sent! Check your mail box.</Alert>
                    ||
                    loadable.error &&
                    <Alert severity="error">{loadable.error}</Alert>}
                </Box>}

                {!loadable.loaded &&
                <StandardForm initialValues={{email: ''} as ValueType}
                              validationSchema={Validation.RESET_PASSWORD_REQUEST}
                              submitButtons={
                                  <>
                                      <Box display="flex">
                                          <Box flexGrow={1}>
                                          </Box>
                                          <Box display="flex" flexGrow={0}>
                                              <Button color="primary" type="submit">Send request</Button>
                                          </Box>
                                      </Box>
                                  </>}
                              onSubmit={(v: ValueType) => {
                                  globalActions.loadableActions.resetPasswordRequest.dispatch(v.email);
                              }}>
                    <TextField name="email" label="E-mail"/>
                </StandardForm>}
            </BaseView>
        </DashboardPage>
    );
};

export default PasswordResetView;