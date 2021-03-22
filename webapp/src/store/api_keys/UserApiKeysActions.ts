import {singleton} from 'tsyringe';
import {AbstractLoadableActions, StateWithLoadables} from "../AbstractLoadableActions";
import {ApiKeysService} from "../../service/apiKeysService";
import {AppState} from "../index";
import {useSelector} from "react-redux";
import {RepositoryService} from "../../service/repositoryService";

export class UserApiKeysState extends StateWithLoadables<UserApiKeysActions> {
}

@singleton()
export class UserApiKeysActions extends AbstractLoadableActions<UserApiKeysState> {

    loadableDefinitions = {
        list: this.createLoadableDefinition(this.apiKeysService.getListForLoggedUser),
        repositories: this.createLoadableDefinition(this.repositoryService.getRepositories),
        scopes: this.createLoadableDefinition(this.apiKeysService.getAvailableScopes),
        generateApiKey: this.createLoadableDefinition(this.apiKeysService.generateApiKey),
        edit: this.createLoadableDefinition(this.apiKeysService.edit),
        delete: this.createLoadableDefinition(this.apiKeysService.delete)
    };

    constructor(private apiKeysService: ApiKeysService, private repositoryService: RepositoryService) {
        super(new UserApiKeysState());
    }

    useSelector<T>(selector: (state: UserApiKeysState) => T): T {
        return useSelector((state: AppState) => selector(state.userApiKey))
    }

    get prefix(): string {
        return "USER_API_KEYS";
    }
}