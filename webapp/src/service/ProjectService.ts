import { container, singleton } from 'tsyringe';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { PermissionDTO, PermissionEditDTO, ProjectDTO } from './response.types';
import { useRedirect } from '../hooks/useRedirect';
import { LINKS } from '../constants/links';
import { components } from './apiSchema.generated';
import { ApiV2HttpService } from './http/ApiV2HttpService';

const http = container.resolve(ApiV1HttpService);

const httpV2 = container.resolve(ApiV2HttpService);

@singleton()
export class ProjectService {
  constructor() {}

  public getV2Projects = async (
    pageable: components['schemas']['Pageable']
  ): Promise<components['schemas']['PagedModelProjectModel']> =>
    httpV2.get(`projects`, pageable);

  public getV2Users = async (
    projectId,
    search,
    pageable: components['schemas']['Pageable']
  ): Promise<components['schemas']['PagedModelUserAccountInProjectModel']> =>
    httpV2.get(`projects/${projectId}/users`, { ...pageable, search });

  public getProjects = async (): Promise<ProjectDTO[]> =>
    await http.get(`projects`);

  public editProject = async (id: number, values: Record<string, unknown>) =>
    (
      await http.postNoJson(`projects/edit`, {
        ...values,
        projectId: id,
      })
    ).json();

  public createProject = async (
    values: components['schemas']['CreateProjectDTO']
  ) => (await http.postNoJson(`projects`, values)).json();

  public deleteProject = async (id) => {
    await http.delete('projects/' + id);
    useRedirect(LINKS.PROJECTS);
  };

  public getPermissions = async (projectId): Promise<PermissionDTO[]> =>
    http.get('permission/list/' + projectId);

  public deletePermission = async (invitationId): Promise<void> => {
    await http.delete('permission/' + invitationId);
  };

  readonly editPermission = async (dto: PermissionEditDTO): Promise<void> =>
    http.post('permission/edit', dto);

  loadProject = (id): Promise<components['schemas']['ProjectModel']> =>
    httpV2.get('projects/' + id);
}
