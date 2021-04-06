import {container, singleton} from 'tsyringe';

import {RepositoryService} from '../../service/RepositoryService';
import {AbstractLoadableActions, StateWithLoadables} from "../AbstractLoadableActions";
import React from "react";
import {OrganizationService} from "../../service/OrganizationService";
import {AppState} from "../index";
import {useSelector} from 'react-redux';
import {components} from "../../service/apiSchema";

export class OrganizationState extends StateWithLoadables<OrganizationActions> {
}

@singleton()
export class OrganizationActions extends AbstractLoadableActions<OrganizationState> {
    constructor(private organizationService: OrganizationService) {
        super(new OrganizationState());
    }

    loadableDefinitions = {
        listPermitted: this.createLoadableDefinition(this.organizationService.getPermitted),
        create: this.createLoadableDefinition(data => this.organizationService.createOrganization(data)),
        get: this.createLoadableDefinition(this.organizationService.getOrganization),
        edit: this.createLoadableDefinition(this.organizationService.editOrganization),
        listAllUsers: this.createLoadableDefinition(this.organizationService.listAllUsers),
        leave: this.createLoadableDefinition(this.organizationService.leave)
    };

    get prefix(): string {
        return 'ORGANIZATIONS';
    }

    useSelector<T>(selector: (state: OrganizationState) => T): T {
        return useSelector((state: AppState) => selector(state.organizations))
    }
}
