import {container, singleton} from 'tsyringe';

import {RepositoryService} from '../../service/RepositoryService';
import {AbstractLoadableActions, createLoadable, StateWithLoadables} from "../AbstractLoadableActions";
import React from "react";
import {OrganizationService} from "../../service/OrganizationService";
import {AppState} from "../index";
import {useSelector} from 'react-redux';
import {components} from "../../service/apiSchema";
import {StateModifier} from "../Action";
import {T} from '@tolgee/react';
import {LINKS} from "../../constants/links";
import {InvitationService} from "../../service/InvitationService";

export class OrganizationState extends StateWithLoadables<OrganizationActions> {
}

@singleton()
export class OrganizationActions extends AbstractLoadableActions<OrganizationState> {
    constructor(private organizationService: OrganizationService, private invitationService: InvitationService) {
        super(new OrganizationState());
    }

    loadableDefinitions = {
        listPermitted: this.createLoadableDefinition(this.organizationService.listPermitted),
        listPermittedForMenu: this.createLoadableDefinition(() => this.organizationService.listPermitted({filterCurrentUserOwner: true, size: 100})),
        listPermittedForRepositoryOwnerSelect: this.createLoadableDefinition(() => this.organizationService.listPermitted({filterCurrentUserOwner: true, size: 100})),
        create: this.createLoadableDefinition(data => this.organizationService.createOrganization(data)),
        get: this.createLoadableDefinition(this.organizationService.getOrganization),
        edit: this.createLoadableDefinition(this.organizationService.editOrganization, (state): OrganizationState => {
            state = this.resetLoadable(state, "edit")
            return this.resetLoadable(state, "get")
        }, <T>organization_updated_message</T>),
        listUsers: this.createLoadableDefinition(this.organizationService.listUsers),
        leave: this.createLoadableDefinition(this.organizationService.leave, (state): OrganizationState => {
            state = this.resetLoadable(state, "leave")
            state = this.resetLoadable(state, "listUsers")
            return this.resetLoadable(state, "listPermitted")
        }, <T>organization_left_message</T>, LINKS.ORGANIZATIONS.build()),
        setRole: this.createLoadableDefinition(this.organizationService.setRole, (state: OrganizationState): OrganizationState => {
            return {...state, loadables: {...state.loadables, listUsers: createLoadable()}} as OrganizationState
        }, <T>organization_role_changed_message</T>),
        invite: this.createLoadableDefinition(this.organizationService.invite, (state: OrganizationState): OrganizationState => {
            return this.resetLoadable(state, "listInvitations")
        }),
        listInvitations: this.createLoadableDefinition(this.organizationService.listInvitations),
        listRepositories: this.createLoadableDefinition(this.organizationService.listRepositories),
        removeUser: this.createLoadableDefinition(this.organizationService.removeUser, (state: OrganizationState): OrganizationState => {
            return this.resetLoadable(state, "listUsers")
        }, <T>organization_user_deleted</T>),
        deleteInvitation: this.createLoadableDefinition(this.invitationService.deleteInvitation, (state: OrganizationState): OrganizationState => {
            return this.resetLoadable(state, "listInvitations")
        })
    };

    get prefix(): string {
        return 'ORGANIZATIONS';
    }

    useSelector<T>(selector: (state: OrganizationState) => T): T {
        return useSelector((state: AppState) => selector(state.organizations))
    }
}
