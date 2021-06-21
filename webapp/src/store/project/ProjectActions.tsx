import { singleton } from 'tsyringe';

import { ProjectDTO } from '../../service/response.types';
import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../AbstractLoadableActions';
import { T } from '@tolgee/react';
import { AppState } from '../index';
import { useSelector } from 'react-redux';
import { ApiSchemaHttpService } from '../../service/http/ApiSchemaHttpService';

export class ProjectsState extends StateWithLoadables<ProjectActions> {
  projects: ProjectDTO[] | undefined = undefined;
}

@singleton()
export class ProjectActions extends AbstractLoadableActions<ProjectsState> {
  constructor(private apiSchemaHttpService: ApiSchemaHttpService) {
    super(new ProjectsState());
  }

  loadableDefinitions = {
    listUsersForPermissions: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/users',
        'get'
      )
    ),
    setUsersPermissions: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/users/{userId}/set-permissions/{permissionType}',
        'put'
      ),
      (state: ProjectsState, action): ProjectsState => {
        return this.resetLoadable(state, 'listUsersForPermissions');
      },
      <T>permissions_set_message</T>
    ),
    revokeAccess: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/users/{userId}/revoke-access',
        'put'
      ),
      (state: ProjectsState, action): ProjectsState => {
        return this.resetLoadable(state, 'listUsersForPermissions');
      },
      <T>access_revoked_message</T>
    ),
  };

  useSelector<T>(selector: (state: ProjectsState) => T): T {
    return useSelector((state: AppState) => selector(state.projects));
  }

  get prefix(): string {
    return 'PROJECTS';
  }
}
