import React, { FunctionComponent, useEffect } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { useHistory, useRouteMatch } from 'react-router-dom';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { CompactView } from 'tg.component/layout/CompactView';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { NewPasswordLabel } from './SetPasswordField';
import { StandardForm } from '../common/form/StandardForm';
import { DashboardPage } from '../layout/DashboardPage';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { messageService } from 'tg.service/MessageService';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

const PasswordFieldWithValidation = React.lazy(
  () => import('tg.component/security/PasswordFieldWithValidation')
);

type ValueType = {
  password: string;
};

const PasswordResetSetView: FunctionComponent = () => {
  const { t } = useTranslate();
  const match = useRouteMatch();
  const encodedData = match.params[PARAMS.ENCODED_EMAIL_AND_CODE];
  const [code, email] = atob(encodedData).split(',');

  const { logout } = useGlobalActions();
  const history = useHistory();

  const passwordResetValidate = useApiQuery({
    url: '/api/public/reset_password_validate/{email}/{code}',
    method: 'get',
    path: {
      code,
      email,
    },
    options: {
      onError(error) {
        messageService.error(<TranslatedError code={error.code!} />);
        history.replace(LINKS.LOGIN.build());
      },
    },
  });

  const passwordResetSet = useApiMutation({
    url: '/api/public/reset_password_set',
    method: 'post',
  });

  const remoteConfig = useConfig();

  const shouldRedirect =
    !remoteConfig.authentication ||
    !remoteConfig.passwordResettable ||
    passwordResetSet.isSuccess;

  useEffect(() => {
    if (shouldRedirect) {
      logout().then(() => {
        history.replace(LINKS.LOGIN.build());
      });
    }
  }, [shouldRedirect]);

  if (shouldRedirect) {
    return <FullPageLoading />;
  }

  return (
    <DashboardPage>
      {passwordResetValidate.isSuccess && (
        <CompactView
          windowTitle={t('reset_password_set_title')}
          title={t('reset_password_set_title')}
          primaryContent={
            <StandardForm
              initialValues={{ password: '' } as ValueType}
              validationSchema={Validation.PASSWORD_RESET(t)}
              submitButtons={
                <LoadingButton
                  color="primary"
                  type="submit"
                  variant="contained"
                  loading={passwordResetSet.isLoading}
                  fullWidth
                  sx={{ mt: 1 }}
                >
                  {t('reset_password_set_submit')}
                </LoadingButton>
              }
              onSubmit={(v: ValueType) => {
                passwordResetSet.mutate(
                  {
                    content: {
                      'application/json': {
                        email,
                        code,
                        password: v.password,
                      },
                    },
                  },
                  {
                    onSuccess() {
                      messageService.success(
                        <T keyName="password_reset_message" />
                      );
                    },
                  }
                );
              }}
            >
              <PasswordFieldWithValidation label={<NewPasswordLabel />} />
            </StandardForm>
          }
        />
      )}
    </DashboardPage>
  );
};

export default PasswordResetSetView;
