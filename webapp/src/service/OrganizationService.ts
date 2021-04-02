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

    public getPermitted = async (): Promise<components["schemas"]["PagedModelOrganizationWithCurrentUserRoleModel"]> => this.v2http.get(`organizations`);

    public createOrganization = (data: components["schemas"]["OrganizationDto"]) =>
        this.v2http.post(`organizations`, data) as Promise<components["schemas"]["OrganizationModel"]>

    public generateAddressPart = (name: String) => this.v2http.post(`address-part/generate-organization`, {name: name}) as Promise<string>
}
