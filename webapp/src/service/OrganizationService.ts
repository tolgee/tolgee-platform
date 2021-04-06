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

    public getPermitted = async (query?: components["schemas"]["Pageable"]):
        Promise<components["schemas"]["PagedModelOrganizationWithCurrentUserRoleModel"]> => this.v2http.get(`organizations`, query);

    public createOrganization = (data: components["schemas"]["OrganizationDto"]) =>
        this.v2http.post(`organizations`, data) as Promise<components["schemas"]["OrganizationModel"]>

    public generateAddressPart = (name: string, oldAddressPart?: string) =>
        this.v2http.post(`address-part/generate-organization`, {name: name, oldAddressPart: oldAddressPart}) as Promise<string>

    public validateAddressPart = (addressPart: String) => this.v2http.get(`address-part/validate-organization/${addressPart}`) as Promise<boolean>

    public getOrganization = (addressPart: string): Promise<components["schemas"]["OrganizationModel"]> => this.v2http.get(`organizations/${addressPart}`);

    public editOrganization = (id, data: components["schemas"]["OrganizationDto"]) =>
        this.v2http.put(`organizations/${id}`, data) as Promise<components["schemas"]["OrganizationModel"]>

    public listAllUsers = async (id: number, query?: components["schemas"]["Pageable"]):
        Promise<components["schemas"]["PagedModelUserAccountWithOrganizationRoleModel"]> => this.v2http.get(`organizations/${id}/users`, query);

    public leave = async (id: number) => this.v2http.put(`organizations/${id}/leave`, {});

}
