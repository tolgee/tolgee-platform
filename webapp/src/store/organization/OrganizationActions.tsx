import {singleton} from 'tsyringe';
import {AbstractLoadableActions, createLoadable, StateWithLoadables,} from '../AbstractLoadableActions';
import React from 'react';
import {OrganizationService} from '../../service/OrganizationService';
import {AppState} from '../index';
import {useSelector} from 'react-redux';
import {T} from '@tolgee/react';
import {LINKS, PARAMS} from '../../constants/links';
import {InvitationService} from '../../service/InvitationService';
import {ApiSchemaHttpService} from '../../service/http/ApiSchemaHttpService';

export class OrganizationState extends StateWithLoadables<OrganizationActions> {}

@singleton()
export class OrganizationActions extends AbstractLoadableActions<OrganizationState> {
  constructor(
    private organizationService: OrganizationService,
    private invitationService: InvitationService,
    private apiSchemaHttpService: ApiSchemaHttpService
  ) {
    super(new OrganizationState());
  }

  loadableDefinitions = {
    listPermitted: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest('/v2/organizations', 'get')
    ),
    listPermittedForMenu: this.createLoadableDefinition(() =>
      this.organizationService.listPermitted({
        filterCurrentUserOwner: true,
        size: 100,
      })
    ),
    listPermittedForProjectOwnerSelect: this.createLoadableDefinition(() =>
      this.organizationService.listPermitted({
        filterCurrentUserOwner: true,
        size: 100,
      })
    ),
    create: this.createLoadableDefinition(
      (data) => this.organizationService.createOrganization(data),
      undefined,
      <T>organization_created_message</T>
    ),
    get: this.createLoadableDefinition(
      this.organizationService.getOrganization
    ),
    edit: this.createLoadableDefinition(
      this.organizationService.editOrganization,
      (state): OrganizationState => {
        state = this.resetLoadable(state, 'edit');
        state = this.resetLoadable(state, 'setMemberPrivileges');
        return this.resetLoadable(state, 'get');
      },
      <T>organization_updated_message</T>,
      (action) => {
        return LINKS.ORGANIZATION_PROFILE.build({
          [PARAMS.ORGANIZATION_ADDRESS_PART]: action.payload.addressPart,
        });
      }
    ),
    setMemberPrivileges: this.createLoadableDefinition(
      this.organizationService.editOrganization,
      (state): OrganizationState => {
        state = this.resetLoadable(state, 'edit');
        state = this.resetLoadable(state, 'setMemberPrivileges');
        return this.resetLoadable(state, 'get');
      },
      <T>organization_member_privileges_set</T>,
      (action) => {
        return LINKS.ORGANIZATION_MEMBER_PRIVILEGES.build({
          [PARAMS.ORGANIZATION_ADDRESS_PART]: action.payload.addressPart,
        });
      }
    ),
    listUsers: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest(
        '/v2/organizations/{id}/users',
        'get'
      )
    ),
    leave: this.createLoadableDefinition(
      this.organizationService.leave,
      (state): OrganizationState => {
        state = this.resetLoadable(state, 'leave');
        state = this.resetLoadable(state, 'listUsers');
        return this.resetLoadable(state, 'listPermitted');
      },
      <T>organization_left_message</T>,
      LINKS.ORGANIZATIONS.build()
    ),
    setRole: this.createLoadableDefinition(
      this.organizationService.setRole,
      (state: OrganizationState): OrganizationState => {
        return {
          ...state,
          loadables: { ...state.loadables, listUsers: createLoadable() },
        } as OrganizationState;
      },
      <T>organization_role_changed_message</T>
    ),
    invite: this.createLoadableDefinition(
      this.organizationService.invite,
      (state: OrganizationState): OrganizationState => {
        return this.resetLoadable(state, 'listInvitations');
      }
    ),
    listInvitations: this.createLoadableDefinition(
      this.organizationService.listInvitations
    ),
    listProjects: this.createLoadableDefinition(
      this.apiSchemaHttpService.schemaRequest(
        '/api/organizations/{addressPart}/projects',
        'get'
      )
    ),
    removeUser: this.createLoadableDefinition(
      this.organizationService.removeUser,
      (state: OrganizationState): OrganizationState => {
        return this.resetLoadable(state, 'listUsers');
      },
      <T>organization_user_deleted</T>
    ),
    deleteInvitation: this.createLoadableDefinition(
      this.invitationService.deleteInvitation,
      (state: OrganizationState): OrganizationState => {
        return this.resetLoadable(state, 'listInvitations');
      }
    ),
    deleteOrganization: this.createLoadableDefinition(
      this.organizationService.deleteOrganization,
      (state: OrganizationState): OrganizationState => {
        state = this.resetLoadable(state, 'listPermitted');
        return this.resetLoadable(state, 'get');
      },
      <T>organization_deleted_message</T>,
      LINKS.ORGANIZATIONS.build()
    ),
  };

  get prefix(): string {
    return 'ORGANIZATIONS';
  }

  useSelector<T>(selector: (state: OrganizationState) => T): T {
    return useSelector((state: AppState) => selector(state.organizations));
  }
}
