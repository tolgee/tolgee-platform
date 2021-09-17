import { FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { Route, Switch, useRouteMatch } from 'react-router-dom';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { FabAddButtonLink } from 'tg.component/common/buttons/FabAddButtonLink';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { AddApiKeyFormDialog } from './AddApiKeyFormDialog';
import { ApiKeysList } from './ApiKeysList';

export const ApiKeysView: FunctionComponent = () => {
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
      <BaseUserSettingsView
        title={<T>Api keys title</T>}
        loading={list.isFetching}
      >
        <>
          {list.isSuccess &&
            ((list.data?.page?.totalElements || 0) < 1 ? (
              <EmptyListMessage>
                <T>No api keys yet!</T>
              </EmptyListMessage>
            ) : (
              <ApiKeysList data={list.data._embedded!.apiKeys!} />
            ))}
          <FabAddButtonLink to={LINKS.USER_API_KEYS_GENERATE.build()} />
        </>
      </BaseUserSettingsView>
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
