import { FC } from 'react';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { T, useTranslate } from '@tolgee/react';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const RequirePreferredOrganization: FC = (props) => {
  const allowPrivate = useGlobalContext((c) => c.auth.allowPrivate);

  const { t } = useTranslate();

  const { preferredOrganization, isFetching } = usePreferredOrganization();

  if (allowPrivate && !preferredOrganization && isFetching) {
    return null;
  }

  if (allowPrivate && !preferredOrganization && !isFetching) {
    return (
      <DashboardPage>
        <CompactView
          content={
            <>
              <T keyName={'no-permissions-on-the-server'} />
            </>
          }
          title={<T keyName={'no-permissions-title'} />}
          windowTitle={t('no-permissions-title')}
        />
      </DashboardPage>
    );
  }

  return <>{props.children}</>;
};
