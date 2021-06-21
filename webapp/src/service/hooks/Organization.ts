import { useMutation, useQuery, UseQueryOptions } from 'react-query';
import { container } from 'tsyringe';
import { ApiV2HttpService } from '../http/ApiV2HttpService';

import { components } from '../apiSchema.generated';

const httpV2 = container.resolve(ApiV2HttpService);

export type OrganizationsParams = components['schemas']['Pageable'] &
  components['schemas']['OrganizationRequestParamsDto'];
export type Organizations =
  components['schemas']['PagedModelOrganizationModel'];
export type OrganizationBody = components['schemas']['OrganizationDto'];
export type Organization = components['schemas']['OrganizationModel'];
export type GenerateSlugBody = components['schemas']['GenerateSlugDto'];
export type PageableQuery = components['schemas']['Pageable'];
export type PageableSearchableQuery = PageableQuery & { search?: string };
export type OrganizationUsers =
  components['schemas']['PagedModelUserAccountWithOrganizationRoleModel'];
export type RoleBody = components['schemas']['SetOrganizationRoleDto'];
export type Invitation = components['schemas']['OrganizationInvitationModel'];
export type InvitationBody = components['schemas']['OrganizationInviteUserDto'];
export type Invitations =
  components['schemas']['CollectionModelOrganizationInvitationModel'];
export type Permission =
  components['schemas']['OrganizationDto']['basePermissions'];
export type PagedProjectModel = components['schemas']['PagedModelProjectModel'];

export const useGetOrganizations = (
  query?: OrganizationsParams,
  options?: UseQueryOptions<Organizations>
) =>
  useQuery<Organizations, any>(
    ['organizations', query],
    () => httpV2.get(`organizations`, query),
    options
  );

export const usePostCreateOrganization = () =>
  useMutation<Organization, any, OrganizationBody>(['organizations'], (data) =>
    httpV2.post(`organizations`, data)
  );

export const usePostGenerateSlug = () =>
  useMutation<string, any, GenerateSlugBody>(
    ['address-part', 'generate-organization'],
    (data) => httpV2.post(`address-part/generate-organization`, data)
  );

export const useGetValidateSlug = (slug: string) =>
  useQuery<string, any>(['address-part', 'validate-organization', slug], () =>
    httpV2.get(`address-part/validate-organization/${slug}`)
  );

export const useGetOrganization = (slug: string) =>
  useQuery<Organization, any>(['organizations', slug], () =>
    httpV2.get(`organizations/${slug}`)
  );

export const usePutOrganization = (id: number) =>
  useMutation<Organization, any, OrganizationBody>(
    ['organizations', id],
    (data) => httpV2.put(`organizations/${id}`, data)
  );

export const useGetOrganizationUsers = (
  id: number,
  query?: PageableSearchableQuery,
  options?: UseQueryOptions<OrganizationUsers>
) =>
  useQuery<OrganizationUsers, any>(
    ['organizations', id, 'users', query],
    () => httpV2.get(`organizations/${id}/users`, query),
    options
  );

export const useDeleteOrganizationUser = (id: number | string) =>
  useMutation<void, any, number>(['organizations', id, 'users'], (userId) =>
    httpV2.delete(`organizations/${id}/users/${userId}`, {})
  );

export const usePutOrganizationLeave = () =>
  useMutation<void, any, number>((id) =>
    httpV2.put(`organizations/${id}/leave`, {})
  );

export const usePutOrganizationRole = (id: number | string, userId: number) =>
  useMutation<void, any, RoleBody>(
    ['organizations', id, 'users', userId, 'set-role'],
    (data) => httpV2.put(`organizations/${id}/users/${userId}/set-role`, data)
  );

export const usePutOrganizationInvite = (id: number | string) =>
  useMutation<Invitation, any, InvitationBody>(
    ['organizations', id, 'invite'],
    (value) => httpV2.put(`organizations/${id}/invite`, value)
  );

export const useGetOrganizationInvitations = (id: number | string) =>
  useQuery<Invitations, any>(['organizations', id, 'invitations'], () =>
    httpV2.get(`organizations/${id}/invitations`)
  );

export const useDeleteOrganization = () =>
  useMutation<void, any, number>(['deleteOrganization'], (id) =>
    httpV2.delete(`organizations/${id}`, {})
  );

export const useGetOrganizationProjects = (
  id: number | string,
  query?: PageableSearchableQuery,
  options?: UseQueryOptions<PagedProjectModel>
) =>
  useQuery<PagedProjectModel, any>(
    ['organizations', id, 'projects', query],
    () => httpV2.get(`organizations/${id}/projects`, query),
    options
  );
