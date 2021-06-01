import { singleton } from 'tsyringe';

import { RepositoryService } from '../../service/RepositoryService';
import { RepositoryDTO } from '../../service/response.types';
import { LINKS } from '../../constants/links';
import {
  AbstractLoadableActions,
  createLoadable,
  Loadable,
  StateWithLoadables,
} from '../AbstractLoadableActions';
import { T } from '@tolgee/react';
import { AppState } from '../index';
import { useSelector } from 'react-redux';
import { ApiV2HttpService } from '../../service/http/ApiV2HttpService';
import { ApiSchemaHttpService } from '../../service/http/ApiSchemaHttpService';

export class RepositoriesState extends StateWithLoadables<RepositoryActions> {
  repositories: RepositoryDTO[] | undefined = undefined;
}

@singleton()
export class RepositoryActions extends AbstractLoadableActions<RepositoriesState> {
  constructor(
    private apiV2HttpService: ApiV2HttpService,
    private apiSchemaHttpService: ApiSchemaHttpService,
    private service: RepositoryService
  ) {
    super(new RepositoriesState());
  }

  loadableDefinitions = {
    listPermitted: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest('/v2/repositories', 'get')
    ),
    listUsersForPermissions: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest(
        '/v2/repositories/{repositoryId}/users',
        'get'
      )
    ),
    editRepository: this.createLoadableDefinition(
      (id, values) => this.service.editRepository(id, values),
      undefined,
      <T>repository_successfully_edited_message</T>,
      LINKS.REPOSITORIES.build()
    ),
    createRepository: this.createLoadableDefinition(
      this.service.createRepository,
      undefined,
      <T>repository_created_message</T>,
      LINKS.REPOSITORIES.build()
    ),
    repository: this.createLoadableDefinition(this.service.loadRepository),
    deleteRepository: this.createLoadableDefinition(
      this.service.deleteRepository,
      (state: RepositoriesState): RepositoriesState => ({
        ...state,
        loadables: {
          ...state.loadables!,
          repository: { ...createLoadable() } as Loadable<RepositoryDTO>,
        },
      }),
      <T>repository_deleted_message</T>
    ),
    setUsersPermissions: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest(
        '/v2/repositories/{repositoryId}/users/{userId}/set-permissions/{permissionType}',
        'put'
      ),
      (state: RepositoriesState, action): RepositoriesState => {
        return this.resetLoadable(state, 'listUsersForPermissions');
      },
      <T>permissions_set_message</T>
    ),
    revokeAccess: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest(
        '/v2/repositories/{repositoryId}/users/{userId}/revoke-access',
        'put'
      ),
      (state: RepositoriesState, action): RepositoriesState => {
        return this.resetLoadable(state, 'listUsersForPermissions');
      },
      <T>access_revoked_message</T>
    ),
  };

  useSelector<T>(selector: (state: RepositoriesState) => T): T {
    return useSelector((state: AppState) => selector(state.repositories));
  }

  get prefix(): string {
    return 'REPOSITORIES';
  }
}
