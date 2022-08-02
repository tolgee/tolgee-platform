import { LINKS } from '../constants/links';
import { redirect } from 'tg.hooks/redirect';
import { components } from './apiSchema.generated';
import { apiV1HttpService } from './http/ApiV1HttpService';
import { apiV2HttpService } from './http/ApiV2HttpService';
import { PermissionDTO, PermissionEditDTO, ProjectDTO } from './response.types';

interface Pageable {
  page: number;
  size: number;
}

export class ProjectService {
  constructor() {}

  public getV2Projects = async (
    pageable: Pageable
  ): Promise<components['schemas']['PagedModelProjectModel']> =>
    apiV2HttpService.get(`projects`, pageable);

  public getV2Users = async (
    projectId,
    search,
    pageable: Pageable
  ): Promise<components['schemas']['PagedModelUserAccountInProjectModel']> =>
    apiV2HttpService.get(`projects/${projectId}/users`, {
      ...pageable,
      search,
    });

  public getProjects = async (): Promise<ProjectDTO[]> =>
    await apiV1HttpService.get(`projects`);

  public editProject = async (id: number, values: Record<string, unknown>) =>
    (
      await apiV1HttpService.postNoJson(`projects/edit`, {
        ...values,
        projectId: id,
      })
    ).json();

  public createProject = async (
    values: components['schemas']['CreateProjectDTO']
  ) => (await apiV1HttpService.postNoJson(`projects`, values)).json();

  public deleteProject = async (id) => {
    await apiV1HttpService.delete('projects/' + id);
    redirect(LINKS.PROJECTS);
  };

  public getPermissions = async (projectId): Promise<PermissionDTO[]> =>
    apiV1HttpService.get('permission/list/' + projectId);

  public deletePermission = async (invitationId): Promise<void> => {
    await apiV1HttpService.delete('permission/' + invitationId);
  };

  readonly editPermission = async (dto: PermissionEditDTO): Promise<void> =>
    apiV1HttpService.post('permission/edit', dto);

  loadProject = (id): Promise<components['schemas']['ProjectModel']> =>
    apiV2HttpService.get('projects/' + id);
}

export const projectService = new ProjectService();
