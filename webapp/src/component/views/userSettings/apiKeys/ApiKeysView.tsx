import { default as React, FunctionComponent, useEffect } from 'react';
import { container } from 'tsyringe';
import { UserApiKeysActions } from '../../../../store/api_keys/UserApiKeysActions';
import { Route, Switch, useRouteMatch } from 'react-router-dom';
import { LINKS, PARAMS } from '../../../../constants/links';
import { FabAddButtonLink } from '../../../common/buttons/FabAddButtonLink';
import { AddApiKeyFormDialog } from './AddApiKeyFormDialog';
import { ApiKeysList } from './ApiKeysList';
import { EmptyListMessage } from '../../../common/EmptyListMessage';
import { T } from '@tolgee/react';
import { BaseUserSettingsView } from '../BaseUserSettingsView';

export const ApiKeysView: FunctionComponent = () => {
  const actions = container.resolve(UserApiKeysActions);

  let list = actions.useSelector((state) => state.loadables.list);

  useEffect(() => {
    if (!list.loading && !list.loaded) {
      actions.loadableActions.list.dispatch();
    }
  }, [list.loaded]);

  const EditForm = () => (
    <>
      {list.loaded && (
        <AddApiKeyFormDialog
          editKey={list.data!.find(
            (key) =>
              key.id === parseInt(useRouteMatch().params[PARAMS.API_KEY_ID])
          )}
        />
      )}
    </>
  );

  //reset loadables after success delete action
  let deleteLoadable = actions.useSelector((state) => state.loadables.delete);
  useEffect(() => {
    if (deleteLoadable.loaded) {
      actions.loadableReset.delete.dispatch();
      actions.loadableReset.list.dispatch();
    }
  }, [deleteLoadable.loaded]);

  return (
    <>
      <BaseUserSettingsView
        title={<T>Api keys title</T>}
        loading={list.loading}
      >
        <>
          {list.loaded &&
            (!list.data!.length ? (
              <EmptyListMessage>
                <T>No api keys yet!</T>
              </EmptyListMessage>
            ) : (
              <ApiKeysList data={list.data!} />
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
