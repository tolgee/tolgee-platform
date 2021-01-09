import {container} from 'tsyringe';
import {GlobalActions} from '../store/global/globalActions';
import {useSelector} from "react-redux";
import {AppState} from "../store";
import {RemoteConfigurationDTO} from "../service/response.types";
import {GlobalError} from "../error/GlobalError";
import {useEffect} from "react";

export const useConfig = (): RemoteConfigurationDTO => {
    let loadable = useSelector((state: AppState) => state.global.loadables.remoteConfig);

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
