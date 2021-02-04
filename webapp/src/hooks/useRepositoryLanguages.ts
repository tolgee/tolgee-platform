import {LanguageDTO} from "../service/response.types";
import {useSelector} from "react-redux";
import {AppState} from "../store";
import {GlobalError} from "../error/GlobalError";

export const useRepositoryLanguages = (): LanguageDTO[] => {
    let languagesLoadable = useSelector((state: AppState) => state.languages.loadables.list);

    if (!languagesLoadable.data) {
        throw new GlobalError("Unexpected error", "No data in loadable? Did you use provider before using hook?")
    }

    return languagesLoadable.data;
};
