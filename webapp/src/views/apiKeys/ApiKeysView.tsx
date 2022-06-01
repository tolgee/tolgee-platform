import { FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Route, Switch, useRouteMatch } from 'react-router-dom';
import { Box } from '@mui/material';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { FabAddButtonLink } from 'tg.component/common/buttons/FabAddButtonLink';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { BaseSettingsView } from '../../component/layout/BaseSettingsView';
import { AddApiKeyFormDialog } from './AddApiKeyFormDialog';
import { ApiKeysList } from './ApiKeysList';

export const ApiKeysView: FunctionComponent = () => {
  const t = useTranslate();
  const list = useApiQuery({
    url: '/v2/api-keys',
    query: {
      pageable: {
        size: 1000,
      },
    },
    method: 'get',
  });

  const EditForm = () => (
    <>
      {list.isSuccess && (
        <AddApiKeyFormDialog
          editKey={list.data?._embedded?.apiKeys?.find(
            (key) =>
              key.id === parseInt(useRouteMatch().params[PARAMS.API_KEY_ID])
          )}
        />
      )}
    </>
  );

  return (
    <>
      <BaseSettingsView
        windowTitle={t('api_keys_title')}
        title={t('api_keys_title')}
        loading={list.isFetching}
        hideChildrenOnLoading={false}
      >
        <>
          {!list.data?.page?.totalElements ? (
            <EmptyListMessage loading={list.isFetching}>
              <T>No api keys yet!</T>
            </EmptyListMessage>
          ) : (
            <ApiKeysList data={list.data._embedded!.apiKeys!} />
          )}
          <Box
            display="flex"
            flexDirection="column"
            alignItems="flex-end"
            mt={2}
            pr={2}
          >
            <FabAddButtonLink to={LINKS.USER_API_KEYS_GENERATE.build()} />
          </Box>
        </>
      </BaseSettingsView>
      <Switch>
        <Route exact path={LINKS.USER_API_KEYS_EDIT.template}>
          <EditForm />
        </Route>
        <Route exact path={LINKS.USER_API_KEYS_GENERATE.template}>
          <AddApiKeyFormDialog />
        </Route>
      </Switch>
    </>
  );
};
