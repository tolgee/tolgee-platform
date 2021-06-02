import {singleton} from 'tsyringe';
import {ApiV2HttpService} from './http/ApiV2HttpService';
import {components} from './apiSchema';

@singleton()
export class OrganizationService {
  constructor(private v2http: ApiV2HttpService) {}

  public listPermitted = async (
    query?: components['schemas']['Pageable'] &
      components['schemas']['OrganizationRequestParamsDto']
  ): Promise<components['schemas']['PagedModelOrganizationModel']> =>
    this.v2http.get(`organizations`, query);

  public createOrganization = (
    data: components['schemas']['OrganizationDto']
  ) =>
    this.v2http.post(`organizations`, data) as Promise<
      components['schemas']['OrganizationModel']
    >;

  public generateSlug = (name: string, oldSlug?: string) =>
    this.v2http.post(`address-part/generate-organization`, {
      name: name,
      oldSlug: oldSlug,
    }) as Promise<string>;

  public validateSlug = (slug: String) =>
    this.v2http.get(
      `address-part/validate-organization/${slug}`
    ) as Promise<boolean>;

  public getOrganization = (
    slug: string
  ): Promise<components['schemas']['OrganizationModel']> =>
    this.v2http.get(`organizations/${slug}`);

  public editOrganization = (
    id,
    data: components['schemas']['OrganizationDto']
  ) =>
    this.v2http.put(`organizations/${id}`, data) as Promise<
      components['schemas']['OrganizationModel']
    >;

  public listUsers = async (
    id: number,
    query?: components['schemas']['Pageable']
  ): Promise<
    components['schemas']['PagedModelUserAccountWithOrganizationRoleModel']
  > => this.v2http.get(`organizations/${id}/users`, query);

  public leave = async (id: number) =>
    this.v2http.put(`organizations/${id}/leave`, {});

  public setRole = async (
    organizationId: number,
    userId: number,
    roleType: string
  ) =>
    this.v2http.put(
      `organizations/${organizationId}/users/${userId}/set-role`,
      { roleType }
    );

  public invite = async (
    organizationId: number,
    roleType: string
  ): Promise<components['schemas']['OrganizationInvitationModel']> =>
    this.v2http.put(`organizations/${organizationId}/invite`, { roleType });

  public listInvitations = async (
    organizationId: number
  ): Promise<
    components['schemas']['CollectionModelOrganizationInvitationModel']
  > => this.v2http.get(`organizations/${organizationId}/invitations`);

  public removeUser = async (organizationId: number, userId: number) =>
    this.v2http.delete(`organizations/${organizationId}/users/${userId}`, {});

  public deleteOrganization = async (organizationId: number) =>
    this.v2http.delete(`organizations/${organizationId}`, {});
}
