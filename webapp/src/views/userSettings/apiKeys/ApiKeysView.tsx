import { T } from '@tolgee/react';
import { FunctionComponent } from 'react';
import { Route, Switch, useRouteMatch } from 'react-router-dom';
import { FabAddButtonLink } from 'tg.component/common/buttons/FabAddButtonLink';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { AddApiKeyFormDialog } from './AddApiKeyFormDialog';
import { ApiKeysList } from './ApiKeysList';

export const ApiKeysView: FunctionComponent = () => {
  const list = useApiQuery({
    url: '/api/apiKeys',
    method: 'get',
  });

  const EditForm = () => (
    <>
      {list.isSuccess && (
        <AddApiKeyFormDialog
          editKey={list.data?.find(
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
            (!list.data?.length ? (
              <EmptyListMessage>
                <T>No api keys yet!</T>
              </EmptyListMessage>
            ) : (
              <ApiKeysList data={list.data} />
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
