import * as React from "react";
import {FunctionComponent} from "react";
import {container} from "tsyringe";
import {useSelector} from "react-redux";
import {AppState} from "../store";
import {GlobalError} from "../error/GlobalError";
import {LanguageActions} from "../store/languages/LanguageActions";
import {useRepository} from "./useRepository";
import {FullPageLoading} from "../component/common/FullPageLoading";

const languageActions = container.resolve(LanguageActions);

export const RepositoryLanguageProvider: FunctionComponent = (props) => {

    let repositoryDTO = useRepository();

    let languagesLoadable = useSelector((state: AppState) => state.languages.loadables.list);

    const isLoading = languagesLoadable.loading;
    const init = !languagesLoadable.data && !languagesLoadable.error && !isLoading;
    const idChanged = languagesLoadable.dispatchParams && languagesLoadable.dispatchParams[0] !== repositoryDTO.id;


    if (init || idChanged) {
        languageActions.loadableActions.list.dispatch(repositoryDTO.id);
        return <FullPageLoading/>
    }

    if (languagesLoadable.loading) {
        return <FullPageLoading/>
    }

    if (languagesLoadable.data) {
        return (
            <>
                {props.children}
            </>
        );
    }

    throw new GlobalError("Unexpected error occurred", languagesLoadable.error.code || "Loadable error");

};