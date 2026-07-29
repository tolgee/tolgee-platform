import { FC } from 'react';
import { Box } from '@mui/material';
import {
  useIsEmailVerified,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { T, useTranslate } from '@tolgee/react';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const RequirePreferredOrganization: FC<
  React.PropsWithChildren<unknown>
> = (props) => {
  const allowPrivate = useGlobalContext((c) => c.auth.allowPrivate);

  const { t } = useTranslate();

  const { preferredOrganization, isFetching } = usePreferredOrganization();

  const isEmailVerified = useIsEmailVerified();
  if (!isEmailVerified) {
    return <>{props.children}</>;
  }
  const hasPrivateAccessWithoutOrganization =
    allowPrivate && !preferredOrganization;

  if (hasPrivateAccessWithoutOrganization) {
    if (isFetching) {
      return null;
    }

    return (
      <DashboardPage>
        <CompactView
          primaryContent={
            <Box data-cy="no-permissions-message">
              <T keyName={'no-permissions-on-the-server'} />
            </Box>
          }
          title={<T keyName={'no-permissions-title'} />}
          windowTitle={t('no-permissions-title')}
        />
      </DashboardPage>
    );
  }

  return <>{props.children}</>;
};
