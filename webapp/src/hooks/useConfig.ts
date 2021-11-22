import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { container } from 'tsyringe';

import { GlobalError } from '../error/GlobalError';
import { AppState } from '../store';
import { GlobalActions } from '../store/global/GlobalActions';
import { components } from 'tg.service/apiSchema.generated';

export const useConfig =
  (): components['schemas']['PublicConfigurationDTO'] => {
    const loadable = useSelector(
      (state: AppState) => state.global.loadables.remoteConfig
    );

    const actions = container.resolve(GlobalActions);

    if (loadable.error) {
      throw new GlobalError(loadable.error.message);
    }

    useEffect(() => {
      if (!loadable.data && !loadable.loading && !loadable.error) {
        actions.loadableActions.remoteConfig.dispatch();
      }
    }, [loadable.loading, loadable.loaded]);

    return loadable.data;
  };
