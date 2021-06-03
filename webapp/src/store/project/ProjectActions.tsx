import { singleton } from 'tsyringe';

import { ProjectService } from '../../service/ProjectService';
import { ProjectDTO } from '../../service/response.types';
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

export class ProjectsState extends StateWithLoadables<ProjectActions> {
  projects: ProjectDTO[] | undefined = undefined;
}

@singleton()
export class ProjectActions extends AbstractLoadableActions<ProjectsState> {
  constructor(
    private apiV2HttpService: ApiV2HttpService,
    private apiSchemaHttpService: ApiSchemaHttpService,
    private service: ProjectService
  ) {
    super(new ProjectsState());
  }

  loadableDefinitions = {
    listPermitted: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest('/v2/projects', 'get')
    ),
    listUsersForPermissions: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/users',
        'get'
      )
    ),
    editProject: this.createLoadableDefinition(
      (id, values) => this.service.editProject(id, values),
      undefined,
      <T>project_successfully_edited_message</T>,
      LINKS.PROJECTS.build()
    ),
    createProject: this.createLoadableDefinition(
      this.service.createProject,
      undefined,
      <T>project_created_message</T>,
      LINKS.PROJECTS.build()
    ),
    project: this.createLoadableDefinition(this.service.loadProject),
    deleteProject: this.createLoadableDefinition(
      this.service.deleteProject,
      (state: ProjectsState): ProjectsState => ({
        ...state,
        loadables: {
          ...state.loadables!,
          project: { ...createLoadable() } as Loadable<ProjectDTO>,
        },
      }),
      <T>project_deleted_message</T>
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
