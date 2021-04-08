import {container, singleton} from 'tsyringe';
import {ApiHttpService} from './ApiHttpService';
import {PermissionDTO, PermissionEditDTO, RepositoryDTO} from './response.types';
import {useRedirect} from "../hooks/useRedirect";
import {LINKS} from "../constants/links";
import {ApiV2HttpService} from "./ApiV2HttpService";
import {components} from "./apiSchema";

@singleton()
export class OrganizationService {

    constructor(private v2http: ApiV2HttpService) {
    }

    public listPermitted = async (query?: components["schemas"]["Pageable"] & components["schemas"]["OrganizationRequestParamsDto"]):
        Promise<components["schemas"]["PagedModelOrganizationWithCurrentUserRoleModel"]> => this.v2http.get(`organizations`, query);

    public createOrganization = (data: components["schemas"]["OrganizationDto"]) =>
        this.v2http.post(`organizations`, data) as Promise<components["schemas"]["OrganizationModel"]>

    public generateAddressPart = (name: string, oldAddressPart?: string) =>
        this.v2http.post(`address-part/generate-organization`, {name: name, oldAddressPart: oldAddressPart}) as Promise<string>

    public validateAddressPart = (addressPart: String) => this.v2http.get(`address-part/validate-organization/${addressPart}`) as Promise<boolean>

    public getOrganization = (addressPart: string): Promise<components["schemas"]["OrganizationModel"]> => this.v2http.get(`organizations/${addressPart}`);

    public editOrganization = (id, data: components["schemas"]["OrganizationDto"]) =>
        this.v2http.put(`organizations/${id}`, data) as Promise<components["schemas"]["OrganizationModel"]>

    public listUsers = async (id: number, query?: components["schemas"]["Pageable"]):
        Promise<components["schemas"]["PagedModelUserAccountWithOrganizationRoleModel"]> => this.v2http.get(`organizations/${id}/users`, query);

    public leave = async (id: number) => this.v2http.put(`organizations/${id}/leave`, {});

    public setRole = async (organizationId: number, userId: number, roleType: string) =>
        this.v2http.put(`organizations/${organizationId}/users/${userId}/set-role`, {roleType});

    public invite = async (organizationId: number, roleType: string): Promise<components["schemas"]["OrganizationInvitationModel"]> =>
        this.v2http.put(`organizations/${organizationId}/invite`, {roleType});

    public listInvitations = async (organizationId: number):
        Promise<components["schemas"]["CollectionModelOrganizationInvitationModel"]> => this.v2http.get(`organizations/${organizationId}/invitations`);

    public listRepositories = async (organizationId: number, query?: components["schemas"]["Pageable"]):
        Promise<components["schemas"]["CollectionModelOrganizationInvitationModel"]> => this.v2http.get(`organizations/${organizationId}/repositories`, query);

    public removeUser = async (organizationId: number, userId: number) =>
        this.v2http.delete(`organizations/${organizationId}/users/${userId}`, {});

    public deleteOrganization = async (organizationId: number) =>
        this.v2http.delete(`organizations/${organizationId}`, {});
}
