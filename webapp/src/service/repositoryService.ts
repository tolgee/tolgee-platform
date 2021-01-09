import {container, singleton} from 'tsyringe';
import {ApiHttpService} from './apiHttpService';
import {PermissionDTO, PermissionEditDTO, RepositoryDTO} from './response.types';
import {useRedirect} from "../hooks/useRedirect";
import {LINKS} from "../constants/links";

const http = container.resolve(ApiHttpService);

@singleton()
export class repositoryService {
    constructor() {
    }

    public getRepositories = async (): Promise<RepositoryDTO[]> => (await http.get(`repositories`));

    public editRepository = async (id: number, values: {}) => (await http.postNoJson(`repositories/edit`,
        {...values, repositoryId: id})).json();

    public createRepository = async (values: Partial<RepositoryDTO>) => (await http.postNoJson(`repositories`, values)).json();

    public deleteRepository = async (id) => {
        await http.delete('repositories/' + id);
        useRedirect(LINKS.REPOSITORIES);
    };

    public getPermissions = async (repositoryId): Promise<PermissionDTO[]> => http.get('permission/list/' + repositoryId);

    public deletePermission = async (invitationId): Promise<void> => {
        await http.delete('permission/' + invitationId);
    };

    readonly editPermission = async (dto: PermissionEditDTO): Promise<void> => http.post('permission/edit', dto);

    loadRepository = (id): Promise<RepositoryDTO> => http.get("repositories/" + id);
}
