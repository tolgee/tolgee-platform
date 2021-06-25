import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { container } from 'tsyringe';
import { GlobalError } from '../error/GlobalError';
import { RemoteConfigurationDTO } from '../service/response.types';
import { AppState } from '../store';
import { GlobalActions } from '../store/global/GlobalActions';

export const useConfig = (): RemoteConfigurationDTO => {
  const loadable = useSelector(
    (state: AppState) => state.global.loadables.remoteConfig
  );

  const actions = container.resolve(GlobalActions);

  if (loadable.error) {
    throw new GlobalError(loadable.error.code);
  }

  useEffect(() => {
    if (!loadable.data && !loadable.loading && !loadable.error) {
      actions.loadableActions.remoteConfig.dispatch();
    }
  }, [loadable.loading, loadable.loaded]);

  return loadable.data;
};
