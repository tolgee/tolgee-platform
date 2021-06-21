import { useMutation, useQuery } from 'react-query';
import { container } from 'tsyringe';

import { ApiV1HttpService } from '../http/ApiV1HttpService';
import { components } from '../apiSchema.generated';

const httpV1 = container.resolve(ApiV1HttpService);

export type ProjectInviteBody = components['schemas']['ProjectInviteUserDto'];

export type Invitation = components['schemas']['InvitationDTO'];

export const usePostInvite = () =>
  useMutation<string, any, ProjectInviteBody>(['projects', 'invite'], (value) =>
    httpV1.post('projects/invite', value)
  );

export const useGetInvitationAccept = (code: string) =>
  useQuery<void, any>(['invitation', 'accept'], () =>
    httpV1.get('invitation/accept/' + code)
  );

export const useGetInvitations = (projectId: number) =>
  useQuery<Invitation[], any>(['invitation', 'list'], () =>
    httpV1.get('invitation/list/' + projectId)
  );

export const useDeleteInvitation = () =>
  useMutation(['invitation'], (invitationId: number) =>
    httpV1.delete('invitation/' + invitationId)
  );
