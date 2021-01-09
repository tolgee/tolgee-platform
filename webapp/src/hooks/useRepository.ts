import {useSelector} from "react-redux";
import {AppState} from "../store";
import {RepositoryDTO} from "../service/response.types";
import {GlobalError} from "../error/GlobalError";

export const useRepository = (): RepositoryDTO => {
    let repositoryDTOLoadable = useSelector((state: AppState) => state.repositories.loadables.repository);

    if (!repositoryDTOLoadable.data) {
        throw new GlobalError("Unexpected error", "No data in loadable? Did you use provider before using hook?")
    }

    return repositoryDTOLoadable.data;
};
