import { container, singleton } from 'tsyringe';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import {
  PermissionDTO,
  PermissionEditDTO,
  RepositoryDTO,
} from './response.types';
import { useRedirect } from '../hooks/useRedirect';
import { LINKS } from '../constants/links';
import { components } from './apiSchema';
import { ApiV2HttpService } from './http/ApiV2HttpService';

const http = container.resolve(ApiV1HttpService);

const httpV2 = container.resolve(ApiV2HttpService);

@singleton()
export class RepositoryService {
  constructor() {}

  public getV2Repositories = async (
    pageable: components['schemas']['Pageable']
  ): Promise<components['schemas']['PagedModelRepositoryModel']> =>
    httpV2.get(`repositories`, pageable);

  public getV2Users = async (
    repositoryId,
    search,
    pageable: components['schemas']['Pageable']
  ): Promise<components['schemas']['PagedModelUserAccountInRepositoryModel']> =>
    httpV2.get(`repositories/${repositoryId}/users`, { ...pageable, search });

  public getRepositories = async (): Promise<RepositoryDTO[]> =>
    await http.get(`repositories`);

  public editRepository = async (id: number, values: {}) =>
    (
      await http.postNoJson(`repositories/edit`, {
        ...values,
        repositoryId: id,
      })
    ).json();

  public createRepository = async (
    values: components['schemas']['CreateRepositoryDTO']
  ) => (await http.postNoJson(`repositories`, values)).json();

  public deleteRepository = async (id) => {
    await http.delete('repositories/' + id);
    useRedirect(LINKS.REPOSITORIES);
  };

  public getPermissions = async (repositoryId): Promise<PermissionDTO[]> =>
    http.get('permission/list/' + repositoryId);

  public deletePermission = async (invitationId): Promise<void> => {
    await http.delete('permission/' + invitationId);
  };

  readonly editPermission = async (dto: PermissionEditDTO): Promise<void> =>
    http.post('permission/edit', dto);

  loadRepository = (id): Promise<components['schemas']['RepositoryModel']> =>
    httpV2.get('repositories/' + id);
}
