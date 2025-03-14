import { FunctionComponent } from 'react';
import { Alert } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Redirect, useHistory } from 'react-router-dom';

import { StandardForm } from 'tg.component/common/form/StandardForm';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useConfig, useUser } from 'tg.globalContext/helpers';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { UserUpdateDTO } from 'tg.service/request.types';
import { UserProfileFields } from './UserProfileFields';
import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { LINKS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';

import { DeleteUserButton } from './DeleteUserButton';

export const UserProfileView: FunctionComponent = () => {
  const { t } = useTranslate();
  const { refetchInitialData } = useGlobalActions();
  const managedBy = useApiQuery({
    url: `/v2/user/managed-by`,
    method: 'get',
  });
  const user = useUser();

  const updateUser = useApiMutation({
    url: '/v2/user',
    method: 'put',
  });

  const handleSubmit = (v: UserUpdateDTO) => {
    if (!v.currentPassword) {
      delete v.currentPassword;
    }

    // @ts-ignore
    v.callbackUrl = window.location.protocol + '//' + window.location.host;
    updateUser.mutate(
      { content: { 'application/json': v } },
      {
        onSuccess() {
          messageService.success(
            <T keyName="User data - Successfully updated!" />
          );
          refetchInitialData();
        },
      }
    );
  };

  const history = useHistory();
  const config = useConfig();
  const isManaged = user?.accountType === 'MANAGED';

  if (!config.authentication) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  return (
    <BaseUserSettingsView
      windowTitle={t('user_profile_title')}
      title={t('user_profile_title')}
      navigation={[[t('user_profile_title'), LINKS.USER_PROFILE.build()]]}
    >
      {isManaged && (
        <Alert severity="info" sx={{ mb: 4 }}>
          {managedBy.isLoading || !managedBy.data ? (
            <T keyName="managed-account-notice" />
          ) : (
            <T
              keyName="managed-account-notice-organization"
              params={{ organization: managedBy.data?.name }}
            />
          )}
        </Alert>
      )}
      {user && (
        <StandardForm
          saveActionLoadable={updateUser}
          customActions={<DeleteUserButton />}
          initialValues={
            {
              name: user.name,
              email: user.username,
              currentPassword: '',
            } as UserUpdateDTO
          }
          validationSchema={Validation.USER_SETTINGS(
            user.accountType,
            user.username
          )}
          onCancel={() => history.goBack()}
          onSubmit={handleSubmit}
        >
          <UserProfileFields />
        </StandardForm>
      )}
    </BaseUserSettingsView>
  );
};
