import {singleton} from 'tsyringe';
import {repositoryService} from '../../../service/repositoryService';
import {AbstractLoadableActions, StateWithLoadables} from "../../AbstractLoadableActions";

export class RepositoryPermissionState extends StateWithLoadables<RepositoryPermissionActions> {
}

@singleton()
export class RepositoryPermissionActions extends AbstractLoadableActions<RepositoryPermissionState> {

    loadableDefinitions = {
        list: this.createLoadableDefinition(this.repositoryService.getPermissions),
        delete: this.createDeleteDefinition("list", async (id) => {
            await this.repositoryService.deletePermission(id);
            return id;
        }),
        edit: this.createLoadableDefinition((this.repositoryService.editPermission), (state) => {
            return this.resetLoadable(state, "list");
        }, "Permission successfully edited!")
    };

    constructor(private repositoryService: repositoryService) {
        super(new RepositoryPermissionState());
    }

    get prefix(): string {
        return 'REPOSITORY_PERMISSION';
    }
}

